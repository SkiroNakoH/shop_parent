package com.atguigu.controller;

import com.atguigu.entity.SkuInfo;
import com.atguigu.feign.CartFeignClient;
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
    @Autowired
    private CartFeignClient cartFeignClient;

    //http://cart.gmall.com/addCart.html?skuId=24&skuNum=1
    @RequestMapping("/addCart.html")
    public String addCart(@RequestParam Long skuId,@RequestParam Integer skuNum,HttpServletRequest request) {
//        String userTempId = request.getHeader("userTempId");

        //远程调用，添加购物车
        cartFeignClient.addCart(skuId,skuNum);

        request.setAttribute("skuInfo", skuDetailFeignClient.getSkuInfo(skuId));
        request.setAttribute("skuNum", skuNum);

        return "cart/addCart";
    }

    @RequestMapping("/cart.html")
    public String cartHtml() {
        return "cart/index";
    }
}
