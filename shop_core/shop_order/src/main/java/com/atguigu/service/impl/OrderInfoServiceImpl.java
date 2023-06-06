package com.atguigu.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.constant.MqConst;
import com.atguigu.entity.OrderDetail;
import com.atguigu.entity.OrderInfo;
import com.atguigu.enums.OrderStatus;
import com.atguigu.enums.ProcessStatus;
import com.atguigu.feign.SkuDetailFeignClient;
import com.atguigu.mapper.OrderInfoMapper;
import com.atguigu.service.OrderDetailService;
import com.atguigu.service.OrderInfoService;
import com.atguigu.util.HttpClientUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 订单表 订单表 服务实现类
 * </p>
 *
 * @author SkiroNakoH
 * @since 2023-05-31
 */
@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {
    @Autowired
    private SkuDetailFeignClient skuDetailFeignClient;
    @Autowired
    private OrderDetailService orderDetailService;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Value("${cancel.order.delay}")
    private Integer cancelOrderDelay;

    @Override
    public StringBuilder checkPriceAndStock(OrderInfo orderInfo) {
        StringBuilder sb = new StringBuilder();
        if (orderInfo != null) {
            List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
            if (!CollectionUtils.isEmpty(orderDetailList)) {
                for (OrderDetail orderDetail : orderDetailList) {
                    //订单价格不相等
                    if (orderDetail.getOrderPrice().compareTo(skuDetailFeignClient.getPrice(orderDetail.getSkuId())) != 0) {
                        sb.append(orderDetail.getSkuName() + "价格出现变化");
                    }

                    //todo： 库存校验
                    String url = "http://localhost:8100/hasStock?skuId=" + orderDetail.getSkuId() + "&num=" + orderDetail.getSkuNum();
                    String result = HttpClientUtil.doGet(url);
                    if ("0".equals(result)) {
                        //没有库存
                        sb.append(orderDetail.getSkuName() + "已售空");
                    }
                }
            }
        }
        return sb;
    }

    @Transactional
    @Override
    public Long saveOrderInfo(OrderInfo orderInfo, String userId) {
        //1. 保存订单基础信息
        orderInfo.setOrderStatus(OrderStatus.UNPAID.name());
        orderInfo.setUserId(Long.valueOf(userId));
        //生成订单交易号  与支付宝交易
        String outTradeNo = "atguigu" + System.currentTimeMillis();
        orderInfo.setOutTradeNo(outTradeNo);

        orderInfo.setTradeBody("异世相遇，尽享美味");
        orderInfo.setCreateTime(new Date());

        //订单的过期时间
        Calendar instance = Calendar.getInstance();
        instance.add(Calendar.MINUTE, 15);
        orderInfo.setExpireTime(instance.getTime());

        orderInfo.setProcessStatus(OrderStatus.UNPAID.name());
        save(orderInfo);

        Long orderId = orderInfo.getId();
        //2. 保存订单详情
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if (!CollectionUtils.isEmpty(orderDetailList)) {
            for (OrderDetail orderDetail : orderDetailList) {
                orderDetail.setOrderId(orderId);
            }
            orderDetailService.saveBatch(orderDetailList);
        }

        //订单超时自动取消
        rabbitTemplate.convertAndSend(MqConst.CANCEL_ORDER_EXCHANGE, MqConst.CANCEL_ORDER_ROUTE_KEY, orderId, correlationData -> {
            correlationData.getMessageProperties().setDelay(cancelOrderDelay);
            return correlationData;
        });

        return orderId;
    }

    @Override
    public OrderInfo getOrderInfoAndOrderDetail(Long orderId) {
        return baseMapper.getOrderInfoAndOrderDetail(orderId);
    }

    @Override
    public void sendOrderInfo2Ware4UpdateStock(OrderInfo orderInfo) {
        //已通知库存系统
        updateOrderStatus(orderInfo, ProcessStatus.NOTIFIED_WARE);

        //封装库存系统需要的参数
        Map<String, Object> map = getWareRequestMap(orderInfo);

        String decreaseStockJSON = JSONObject.toJSONString(map);
        //通过mq通知库存系统删减库存
        rabbitTemplate.convertAndSend(MqConst.DECREASE_STOCK_EXCHANGE, MqConst.DECREASE_STOCK_ROUTE_KEY, decreaseStockJSON);

    }

    @Override
    public void updateOrderStatus(OrderInfo orderInfo, ProcessStatus paid) {
        orderInfo.setOrderStatus(paid.name());
        orderInfo.setProcessStatus(paid.name());
        updateById(orderInfo);
    }


    //拆单
    @Transactional
    @Override
    public String splitOrder(Map<String, String> map) {
        String orderId = map.get("orderId");
        //获取订单
        OrderInfo parentOrderInfo = getOrderInfoAndOrderDetail(Long.valueOf(orderId));

        String wareSkuMapJSON = map.get("wareHouseIdSkuIdMapJson");
//        [{"wareId":"1","skuIds":["2","10"]},{"wareId":"2","skuIds":["3"]}]
        List<Map> wareSkuMapList = JSONArray.parseArray(wareSkuMapJSON, Map.class);
        //父订单详情表
        List<OrderDetail> orderDetailList = parentOrderInfo.getOrderDetailList();
        //封装返回的参数
        List<Map> retList = new ArrayList<>();

        //拆单
        for (Map wareSkuMap : wareSkuMapList) {
            OrderInfo childOrderInfo = new OrderInfo();
            String wareId = (String) wareSkuMap.get("wareHouseId");
            //转换
            BeanUtils.copyProperties(parentOrderInfo, childOrderInfo);
            //设置自身id = null  重要
            childOrderInfo.setId(null);
            childOrderInfo.setCreateTime(new Date());
            childOrderInfo.setParentOrderId(parentOrderInfo.getId());
            childOrderInfo.setWareHouseId(wareId);

            //设置孩子订单详情
            BigDecimal childOrderInfoTotalMoney = new BigDecimal("0");
            List<OrderDetail> childOrderDetailList = new ArrayList<>();
            //此仓库拥有的商品
            List<String> wareSkuList = (List<String>) wareSkuMap.get("skuIdList");
            for (OrderDetail orderDetail : orderDetailList) {
                for (String wareSkuId : wareSkuList) {
                    if (orderDetail.getSkuId() == Long.valueOf(wareSkuId)) {
                        OrderDetail childOrderDetail = new OrderDetail();
                        BeanUtils.copyProperties(orderDetail, childOrderDetail);
                        //获得价格
                        childOrderInfoTotalMoney.add(childOrderDetail.getOrderPrice().multiply(new BigDecimal(childOrderDetail.getSkuNum())));
                        //修改孩子详情的所属订单id
                        childOrderDetail.setOrderId(childOrderInfo.getId());
                        //存储进当前孩子订单中
                        childOrderDetailList.add(orderDetail);
                    }
                }
            }
            childOrderInfo.setTotalMoney(childOrderInfoTotalMoney);

            //保存订单
            save(childOrderInfo);

            //修改孩子详情的所属订单id
          /*  childOrderDetailList = childOrderDetailList.stream().map(childDetail -> {
                childDetail.setOrderId(childOrderInfo.getId());
                return childDetail;
            }).collect(Collectors.toList());*/

            childOrderInfo.setOrderDetailList(childOrderDetailList);
            //保存孩子
            orderDetailService.saveBatch(childOrderDetailList);

            //获取返回参数
            retList.add(getWareRequestMap(childOrderInfo));
        }
        //修改父订单的状态
        updateOrderStatus(parentOrderInfo, ProcessStatus.SPLIT);


        return JSONObject.toJSONString(retList);
    }

    //封装库存系统所需要的参数
    private static Map<String, Object> getWareRequestMap(OrderInfo orderInfo) {
        Map<String, Object> map = new HashMap<>();
        map.put("orderId", orderInfo.getId());
        map.put("consignee", orderInfo.getConsignee());
        map.put("consigneeTel", orderInfo.getConsigneeTel());
        map.put("orderComment", orderInfo.getOrderComment());
        map.put("orderBody", orderInfo.getTradeBody());
        map.put("deliveryAddress", orderInfo.getDeliveryAddress());
        map.put("paymentWay", 2);
        //拆单时所需要的仓库id
        if (!StringUtils.isEmpty(orderInfo.getWareHouseId())) {
            map.put("wareId", orderInfo.getWareHouseId());
        }

        List<Map<String, Object>> wareDetailList = orderInfo.getOrderDetailList().stream().map(orderDetail -> {
            Map<String, Object> detailMap = new HashMap<>();
            detailMap.put("skuName", orderDetail.getSkuName());
            detailMap.put("skuId", orderDetail.getSkuId());
            detailMap.put("skuNum", orderDetail.getSkuNum());
            return detailMap;
        }).collect(Collectors.toList());
        map.put("details", wareDetailList);

        return map;
    }
}
