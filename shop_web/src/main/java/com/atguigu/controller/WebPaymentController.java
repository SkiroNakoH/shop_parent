package com.atguigu.controller;

import com.atguigu.entity.OrderInfo;
import com.atguigu.feign.CartFeignClient;
import com.atguigu.feign.OrderFeignClient;
import com.atguigu.feign.SkuDetailFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
public class WebPaymentController {
    @Autowired
    private OrderFeignClient orderFeignClient;

    // /pay.html?orderId=122
    @RequestMapping("/pay.html")
    public String payHtml(@RequestParam Long orderId, Model model) {
        OrderInfo orderInfo = orderFeignClient.getOrderInfoAndOrderDetail(orderId);
        model.addAttribute("orderInfo", orderInfo);
        return "payment/pay";
    }

    //同步回调地址 跳转到支付成功页面  http://payment.gmall.com/alipay/success.html
    @RequestMapping("/alipay/success.html")
    public String successHtml() {
        //同步返回的不可靠性，支付结果必须以异步通知或查询接口返回为准
        return "payment/success";
    }

}
