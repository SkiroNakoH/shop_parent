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
@FeignClient("shop-search")
public interface SearchFeignClient {
    @GetMapping("/search/onSale/{skuId}")
    public RetVal onSale(@PathVariable Long skuId);

    @GetMapping("/search/offSale/{skuId}")
    public RetVal offSale(@PathVariable Long skuId);

    @PostMapping("/search/searchProduct")
    public RetVal searchProduct(@RequestBody SearchParam searchParam);

    @GetMapping("/search/incrHotScore/{skuId}")
    public String incrHotScore(@PathVariable Long skuId);
}
