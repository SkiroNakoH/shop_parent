package com.atguigu.feign;

import com.atguigu.result.RetVal;
import com.atguigu.search.SearchParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Component
@FeignClient("shop-cart")
public interface CartFeignClient {
    @GetMapping("/cart/addCart/{skuId}/{skuNum}")
    public void addCart(@PathVariable Long skuId, @PathVariable Integer skuNum);
}
