package com.atguigu;

import com.atguigu.entity.BaseCategoryView;
import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.entity.SkuInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@FeignClient("shop-product")
public interface SkuDetailFeignClient {
    //1.获取商品基本信息，包括商品图片
    @GetMapping("/sku/getSkuInfo/{skuId}")
    public SkuInfo getSkuInfo(@PathVariable Long skuId);

    //2.获取商品分类
    @GetMapping("/sku/getCategoryView/{categroy3Id}")
    public BaseCategoryView getCategoryView(@PathVariable Long categroy3Id);

    //3.获取商品价格
    @GetMapping("/sku/getPrice/{skuId}")
    public BigDecimal getPrice(@PathVariable Long skuId);

    //4.获取商品属性和商品id的映射关系
    @GetMapping("/sku/getSalePropertyAndSkuMapping/{productId}")
    public Map getSalePropertyAndSkuMapping(@PathVariable Long productId);

    //5.获取商品属性
    @GetMapping("/sku/getSalePropertyAndSkuMapping/{productId}/{skuId}")
    public List<ProductSalePropertyKey> getSpuSalePropertyList(@PathVariable Long productId,
                                                               @PathVariable Long skuId);
}
