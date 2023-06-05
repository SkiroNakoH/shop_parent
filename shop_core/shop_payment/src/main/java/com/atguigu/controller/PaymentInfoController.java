package com.atguigu.controller;


import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.atguigu.service.PaymentInfoService;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 支付信息表 前端控制器
 * </p>
 *
 * @author SkiroNakoH
 * @since 2023-06-05
 */
@RestController
@RequestMapping("/payment")
public class PaymentInfoController {
    @Autowired
    private PaymentInfoService paymentInfoService;

    //    /payment/createQrCode/{orderId}
    @GetMapping("/createQrCode/{orderId}")
    public String createQrCode(@PathVariable Long orderId) {
        return paymentInfoService.createQrCode(orderId);
    }
}

