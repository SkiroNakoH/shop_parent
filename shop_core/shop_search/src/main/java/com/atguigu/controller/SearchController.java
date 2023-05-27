package com.atguigu.controller;

import com.atguigu.result.RetVal;
import com.atguigu.search.Product;
import com.atguigu.search.SearchBrandVo;
import com.atguigu.search.SearchParam;
import com.atguigu.search.SearchResponseVo;
import com.atguigu.service.ProductSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.web.bind.annotation.*;

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
    public RetVal onSale(@PathVariable Long skuId){
        productSearchService.onSale(skuId);
        return RetVal.ok();
    }

    @GetMapping("/offSale/{skuId}")
    public RetVal offSale(@PathVariable Long skuId){
        productSearchService.offSale(skuId);
        return RetVal.ok();
    }

    @PostMapping("/searchProduct")
    public RetVal searchProduct(SearchParam searchParam){
        SearchResponseVo searchResponseVo = productSearchService.searchProduct(searchParam);
        return RetVal.ok(searchResponseVo);
    }
}