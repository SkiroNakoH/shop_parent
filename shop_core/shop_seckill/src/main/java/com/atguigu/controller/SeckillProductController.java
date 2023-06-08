package com.atguigu.controller;


import com.atguigu.entity.SeckillProduct;
import com.atguigu.result.RetVal;
import com.atguigu.service.SeckillProductService;
import com.atguigu.util.UserIdUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author SkiroNakoH
 * @since 2023-06-08
 */
@RestController
@RequestMapping("/seckill")
public class SeckillProductController {
    @Autowired
    private SeckillProductService seckillProductService;

    //查询秒杀上架的商品
    @GetMapping("/queryAllSeckill")
    public List<SeckillProduct> queryAllSeckill(){
        return seckillProductService.queryAllSeckill();
    }

    //秒杀商品详情
    @GetMapping("/{skuId}")
    public SeckillProduct querySecKillBySkuId(@PathVariable Long skuId){
        return seckillProductService.querySecKillBySkuId(skuId);
    }

    //生成抢购码     http://api.gmall.com/seckill/generateSeckillCode/33
    @GetMapping("/generateSeckillCode/{skuId}")
    public RetVal generateSeckillCode(@PathVariable Long skuId, HttpServletRequest request){
        return seckillProductService.generateSeckillCode(skuId,request);
    }

}

