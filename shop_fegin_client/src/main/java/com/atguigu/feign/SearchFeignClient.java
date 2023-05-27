package com.atguigu.feign;

import com.atguigu.result.RetVal;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Component
@FeignClient("shop-search")
public interface SearchFeignClient {
    @GetMapping("/search/onSale/{skuId}")
    public RetVal onSale(@PathVariable Long skuId);

    @GetMapping("/search/offSale/{skuId}")
    public RetVal offSale(@PathVariable Long skuId);
}