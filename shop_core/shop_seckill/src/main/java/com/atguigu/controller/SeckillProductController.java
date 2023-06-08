package com.atguigu.controller;


import com.atguigu.entity.SeckillProduct;
import com.atguigu.service.SeckillProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/querySeckill")
    public List<SeckillProduct> querySeckill(){
        return seckillProductService.querySeckill();
    }

}

