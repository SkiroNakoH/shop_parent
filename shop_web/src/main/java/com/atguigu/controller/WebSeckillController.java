package com.atguigu.controller;

import com.atguigu.entity.SeckillProduct;
import com.atguigu.feign.CartFeignClient;
import com.atguigu.feign.SeckillFeignClient;
import com.atguigu.feign.SkuDetailFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
public class WebSeckillController {
    @Autowired
    private SeckillFeignClient seckillFeignClient;

    @RequestMapping("/seckill-index.html")
    public String seckillIndex(Model model) {
        //查询秒杀列表
        List<SeckillProduct> seckillProductList = seckillFeignClient.querySeckill();
        if (CollectionUtils.isEmpty(seckillProductList))
            return "seckill/fail";

        model.addAttribute("list", seckillProductList);
        return "seckill/index";
    }
}
