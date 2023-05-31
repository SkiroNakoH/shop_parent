package com.atguigu.controller;

import com.atguigu.feign.OrderFeignClient;
import com.atguigu.result.RetVal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
public class WebOrderController {
    @Autowired
    private OrderFeignClient orderFeignClient;

    // http://order.gmall.com/confirm.html
    @RequestMapping("/confirm.html")
    public String confirmHtml(Model model) {

        //通过远程调用shop-order拿到订单信息
        RetVal<Map> retVal = orderFeignClient.confirm();
        model.addAllAttributes(retVal.getData());
        return "order/confirm";
    }


}