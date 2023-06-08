package com.atguigu.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeCloseResponse;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.atguigu.config.AlipayConfig;
import com.atguigu.constant.MqConst;
import com.atguigu.entity.OrderInfo;
import com.atguigu.entity.PaymentInfo;
import com.atguigu.enums.PaymentStatus;
import com.atguigu.enums.PaymentType;
import com.atguigu.feign.OrderFeignClient;
import com.atguigu.mapper.PaymentInfoMapper;
import com.atguigu.service.PaymentInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.SneakyThrows;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

/**
 * <p>
 * 支付信息表 服务实现类
 * </p>
 *
 * @author SkiroNakoH
 * @since 2023-06-05
 */
@Service
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentInfoService {
    @Autowired
    private AlipayClient alipayClient;
    @Autowired
    private OrderFeignClient orderFeignClient;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @SneakyThrows
    @Override
    public String createQrCode(Long orderId) {
        OrderInfo orderInfo = orderFeignClient.getOrderInfoAndOrderDetail(orderId);
        if (orderInfo == null)
            return null;

        // 保存支付相关的信息
        savePaymentInfo(orderInfo);
        //调用支付宝提供的接口，请求支付二维码
//        AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do", "app_id", "your private_key", "json", "GBK", "alipay_public_key", "RSA2");
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        //异步接收地址，仅支持http/https，公网可访问
        request.setNotifyUrl(AlipayConfig.notify_payment_url);
        //同步跳转地址，仅支持http/https
        request.setReturnUrl(AlipayConfig.return_payment_url);
        /******必传参数******/
        JSONObject bizContent = new JSONObject();
        //商户订单号，商家自定义，保持唯一性
        bizContent.put("out_trade_no", orderInfo.getOutTradeNo());
        //支付金额，最小值0.01元
        bizContent.put("total_amount", orderInfo.getTotalMoney());
        //订单标题，不可使用特殊符号
        bizContent.put("subject", orderInfo.getTradeBody());
        //电脑网站支付场景固定传值FAST_INSTANT_TRADE_PAY
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");

        /******可选参数******/
        /*//bizContent.put("time_expire", "2022-08-01 22:00:00");

        //// 商品明细信息，按需传入
        //JSONArray goodsDetail = new JSONArray();
        //JSONObject goods1 = new JSONObject();
        //goods1.put("goods_id", "goodsNo1");
        //goods1.put("goods_name", "子商品1");
        //goods1.put("quantity", 1);
        //goods1.put("price", 0.01);
        //goodsDetail.add(goods1);
        //bizContent.put("goods_detail", goodsDetail);

        //// 扩展信息，按需传入
        //JSONObject extendParams = new JSONObject();
        //extendParams.put("sys_service_provider_id", "2088511833207846");
        //bizContent.put("extend_params", extendParams);*/

        request.setBizContent(bizContent.toString());
        AlipayTradePagePayResponse response = alipayClient.pageExecute(request);
        if (response.isSuccess()) {
//            System.out.println("调用成功");
            return response.getBody();
        } else {
            System.out.println("调用失败");
            return null;
        }
    }

    @Override
    public void updatePayment(Map<String, String> alipayParam) {
        String outTradeNo = alipayParam.get("out_trade_no");
        PaymentInfo paymentInfo = getPaymentInfoByOutTradeNo(outTradeNo);

        if (paymentInfo == null)
            return;

        paymentInfo.setTradeNo(alipayParam.get("trade_no"));
        paymentInfo.setPaymentStatus(PaymentStatus.PAID.name());
        paymentInfo.setCallbackTime(new Date());
        paymentInfo.setCallbackContent(JSONObject.toJSONString(alipayParam));
        updateById(paymentInfo);

        //通过mq，百分百投递，修改订单的状态
        rabbitTemplate.convertAndSend(MqConst.PAY_ORDER_EXCHANGE, MqConst.PAY_ORDER_ROUTE_KEY, paymentInfo.getOrderId());
    }

    //退款接口
    @SneakyThrows
    @Override
    public boolean refund(Long orderId) {
        OrderInfo orderInfo = orderFeignClient.getOrderInfoAndOrderDetail(orderId);
        if (orderInfo == null)
            return false;

        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        JSONObject bizContent = new JSONObject();
        String outTradeNo = orderInfo.getOutTradeNo();
        bizContent.put("out_trade_no", outTradeNo);
        bizContent.put("refund_amount", orderInfo.getTotalMoney());
//        bizContent.put("out_request_no", "HZ01RF001");

        //// 返回参数选项，按需传入
        /*//JSONArray queryOptions = new JSONArray();
        //queryOptions.add("refund_detail_item_list");
        //bizContent.put("query_options", queryOptions);*/

        request.setBizContent(bizContent.toString());
        AlipayTradeRefundResponse response = alipayClient.execute(request);
        if (response.isSuccess()) {
            //修改支付状态 -->    改为订单关闭
            PaymentInfo paymentInfo = getPaymentInfoByOutTradeNo(outTradeNo);
            if (paymentInfo != null) {
                paymentInfo.setPaymentStatus(PaymentStatus.ClOSED.name());
                updateById(paymentInfo);
            }

            System.out.println("调用成功");
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }
    }

    //查询支付宝中是否有交易记录
    @SneakyThrows
    @Override
    public Boolean queryAlipayTrade(Long orderId) {
        String outTradeNo = getOutTradeNo(orderId);
        if (outTradeNo == null) return false;

        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", outTradeNo);

        request.setBizContent(bizContent.toString());
        AlipayTradeQueryResponse response = alipayClient.execute(request);

        if (response.isSuccess()) {
            System.out.println("订单存在");
            return true;
        } else {
            System.out.println("订单不存在");
            return false;
        }
    }

    //    关闭交易
    @SneakyThrows
    @Override
    public Boolean closeAlipayTrade(Long orderId) {
        String outTradeNo = getOutTradeNo(orderId);
        if (outTradeNo == null) return false;

        AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
        JSONObject bizContent = new JSONObject();

        bizContent.put("out_trade_no", outTradeNo);
        request.setBizContent(bizContent.toString());
        AlipayTradeCloseResponse response = alipayClient.execute(request);
        if (response.isSuccess()) {
            return true;
        } else {
            return false;
        }
    }

    //修改订单状态
    @Override
    public void updatePaymentStatus(String outTradeNo, PaymentStatus paymentStatus) {
        PaymentInfo paymentInfo = getPaymentInfoByOutTradeNo(outTradeNo);
        if(paymentInfo != null){
            paymentInfo.setPaymentStatus(paymentStatus.name());
            updateById(paymentInfo);
        }
    }

    private String getOutTradeNo(Long orderId) {
        OrderInfo orderInfo = orderFeignClient.getOrderInfoAndOrderDetail(orderId);
        if (orderInfo == null)
            return null;
        return orderInfo.getOutTradeNo();
    }

    private PaymentInfo getPaymentInfoByOutTradeNo(String outTradeNo) {
        LambdaQueryWrapper<PaymentInfo> paymentInfoLambdaQueryWrapper = new LambdaQueryWrapper<>();
        paymentInfoLambdaQueryWrapper.eq(PaymentInfo::getOutTradeNo, outTradeNo);
        //支付方式
        paymentInfoLambdaQueryWrapper.eq(PaymentInfo::getPaymentType, PaymentType.ALIPAY);
        PaymentInfo paymentInfo = getOne(paymentInfoLambdaQueryWrapper);
        return paymentInfo;
    }

    //保存订单，防止跟用户扯皮
    private void savePaymentInfo(OrderInfo orderInfo) {
        LambdaQueryWrapper<PaymentInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentInfo::getOrderId, orderInfo.getId());
        wrapper.eq(PaymentInfo::getPaymentType, PaymentType.ALIPAY.name());
        int count = count(wrapper);
        if (count > 0) {
            return;
        }
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setOrderId(orderInfo.getId() + "");
        paymentInfo.setPaymentType(PaymentType.ALIPAY.name());
        paymentInfo.setPaymentMoney(orderInfo.getTotalMoney());
        paymentInfo.setPaymentContent(orderInfo.getTradeBody());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID.name());
        paymentInfo.setCreateTime(new Date());
        save(paymentInfo);
    }
}
