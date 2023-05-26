package com.atguigu.controller;

import com.atguigu.result.RetVal;
import com.atguigu.search.Product;
import com.atguigu.service.ProductSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/search")
public class SearchController {
    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;
    @Autowired
    private ProductSearchService productSearchService;

    @GetMapping("/create")
    public String createIndex() {
        //创建索引
        elasticsearchRestTemplate.createIndex(Product.class);
        //创建映射
        elasticsearchRestTemplate.putMapping(Product.class);
        return "success";
    }

    @GetMapping("/onSale/{skuId}")
    public RetVal onsale(@PathVariable Long skuId){
        productSearchService.onsale(skuId);
        return RetVal.ok();
    }

    @GetMapping("/offSale/{skuId}")
    public RetVal offSale(@PathVariable Long skuId){
        productSearchService.offSale(skuId);
        return RetVal.ok();
    }
}
