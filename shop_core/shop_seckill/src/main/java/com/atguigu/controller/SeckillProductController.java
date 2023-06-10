package com.atguigu.controller;


import com.atguigu.entity.SeckillProduct;
import com.atguigu.result.RetVal;
import com.atguigu.service.SeckillProductService;
import com.atguigu.util.UserIdUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
    public List<SeckillProduct> queryAllSeckill() {
        return seckillProductService.queryAllSeckill();
    }

    //秒杀商品详情
    @GetMapping("/{skuId}")
    public SeckillProduct querySecKillBySkuId(@PathVariable Long skuId) {
        return seckillProductService.querySecKillBySkuId(skuId);
    }

    //生成抢购码     http://api.gmall.com/seckill/generateSeckillCode/33
    @GetMapping("/generateSeckillCode/{skuId}")
    public RetVal generateSeckillCode(@PathVariable Long skuId, HttpServletRequest request) {
        return seckillProductService.generateSeckillCode(skuId, request);
    }

    // http://api.gmall.com/seckill/prepareSeckill/24?seckillCode=eccbc87e4b5ce2fe28308fd9f2a7baf3
    //预下单
    @PostMapping("/prepareSeckill/{skuId}")
    public RetVal prepareSeckill(@PathVariable Long skuId, String seckillCode, HttpServletRequest request) {
        return seckillProductService.prepareSeckill(skuId,seckillCode,request);
    }

    // http://api.gmall.com/seckill/hasQualified/33
    @GetMapping("/hasQualified/{skuId}")
    public RetVal hasQualified(@PathVariable Long skuId,HttpServletRequest request){

        return seckillProductService.hasQualified(skuId,request);
    }

}

