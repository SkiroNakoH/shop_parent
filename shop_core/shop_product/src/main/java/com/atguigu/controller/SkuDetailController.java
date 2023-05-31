package com.atguigu.controller;

import com.atguigu.entity.BaseCategoryView;
import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.entity.SkuInfo;
import com.atguigu.mapper.ProductSalePropertyKeyMapper;
import com.atguigu.service.BaseCategoryViewService;
import com.atguigu.service.SkuInfoService;
import com.atguigu.service.SkuSalePropertyValueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RequestMapping("/sku")
@RestController
public class SkuDetailController {
    @Autowired
    private SkuInfoService skuInfoService;
    @Autowired
    private BaseCategoryViewService categoryViewService;
    @Autowired
    private SkuSalePropertyValueService skuSalePropertyValueService;
    @Autowired
    private ProductSalePropertyKeyMapper propertyKeyMapper;

    //1.获取商品基本信息，包括商品图片
    @GetMapping("/getSkuInfo/{skuId}")
    public SkuInfo getSkuInfo(@PathVariable Long skuId) {
        return skuInfoService.getSkuInfo(skuId);
    }

    //2.获取商品分类
    @GetMapping("/getCategoryView/{categroy3Id}")
    public BaseCategoryView getCategoryView(@PathVariable Long categroy3Id) {
        return categoryViewService.getById(categroy3Id);
    }

    //3.获取商品价格
    @GetMapping("/getPrice/{skuId}")
    public BigDecimal getPrice(@PathVariable Long skuId) {
        return skuInfoService.getInfoFromDB(skuId).getPrice();
    }

    //4.获取商品属性和商品id的映射关系
    @GetMapping("/getSalePropertyAndSkuMapping/{productId}")
    public Map getSalePropertyAndSkuMapping(@PathVariable Long productId) {
        return skuSalePropertyValueService.getSalePropertyAndSkuMapping(productId);
    }

    //5.获取商品属性
    @GetMapping("/getSalePropertyAndSkuMapping/{productId}/{skuId}")
    public List<ProductSalePropertyKey> getSpuSalePropertyList(@PathVariable Long productId,
                                                               @PathVariable Long skuId) {

        return propertyKeyMapper.getSpuSalePropertyList(productId,skuId);
    }

}
