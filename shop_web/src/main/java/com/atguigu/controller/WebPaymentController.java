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
}
