package com.atguigu.controller;

import com.atguigu.entity.SkuInfo;
import com.atguigu.feign.SkuDetailFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;

@Controller
public class WebCartController {
    @Autowired
    private SkuDetailFeignClient skuDetailFeignClient;

    //http://cart.gmall.com/addCart.html?skuId=24&skuNum=1
    @RequestMapping("/addCart.html")
    public String addCart(HttpServletRequest request) {
        Long skuId = Long.valueOf(request.getParameter("skuId"));
        Integer skuNum = Integer.valueOf(request.getParameter("skuNum"));

        SkuInfo skuInfo = skuDetailFeignClient.getSkuInfo(skuId);
        request.setAttribute("skuInfo",skuInfo);
        request.setAttribute("skuNum",skuNum);

        return "cart/addCart";
    }

}
