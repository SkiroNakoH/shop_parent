package com.atguigu.service.impl;

import com.atguigu.entity.OrderDetail;
import com.atguigu.entity.OrderInfo;
import com.atguigu.enums.OrderStatus;
import com.atguigu.feign.SkuDetailFeignClient;
import com.atguigu.mapper.OrderInfoMapper;
import com.atguigu.service.OrderDetailService;
import com.atguigu.service.OrderInfoService;
import com.atguigu.util.HttpClientUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

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
        //生成订单交易号 todo： 与支付宝交易
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

        return orderId;
    }
}
