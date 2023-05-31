package com.atguigu.feign;

import com.atguigu.entity.CartInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient("shop-cart")
public interface CartFeignClient {
    //页面点击添加购物车
    @GetMapping("/cart/addCart/{skuId}/{skuNum}")
    public void addCart(@PathVariable Long skuId, @PathVariable Integer skuNum);

    //查看被选中的商品列表
    @GetMapping("/cart/getSelectedCartInfo/{userId}")
    public List<CartInfo> getSelectedCartInfo(@PathVariable String userId);
}