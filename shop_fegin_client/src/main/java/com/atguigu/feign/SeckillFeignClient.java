package com.atguigu.feign;

import com.atguigu.entity.SeckillProduct;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient("shop-seckill")
public interface SeckillFeignClient {
    @GetMapping("/seckill/querySeckill")
    public List<SeckillProduct> querySeckill();
}