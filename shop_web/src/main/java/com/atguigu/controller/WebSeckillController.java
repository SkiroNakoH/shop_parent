package com.atguigu.controller;

import com.atguigu.entity.SeckillProduct;
import com.atguigu.feign.CartFeignClient;
import com.atguigu.feign.SeckillFeignClient;
import com.atguigu.feign.SkuDetailFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
        List<SeckillProduct> seckillProductList = seckillFeignClient.queryAllSeckill();
        if (CollectionUtils.isEmpty(seckillProductList))
            return "seckill/fail";

        model.addAttribute("list", seckillProductList);
        return "seckill/index";
    }

    //秒杀商品的详情页面
    @RequestMapping("seckill-detail/{skuId}.html")
    public String seckillDetail(@PathVariable Long skuId, Model model) {
        SeckillProduct seckillProduct = seckillFeignClient.querySecKillBySkuId(skuId);
        model.addAttribute("item",seckillProduct);
        return "seckill/detail";
    }

    @RequestMapping("/seckill-queue.html")
    public String seckillQueue(Long skuId,String seckillCode,Model model) {
        model.addAttribute("skuId",skuId);
        model.addAttribute("seckillCode",seckillCode);

        return "seckill/queue";
    }
}
