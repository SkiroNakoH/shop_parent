package com.atguigu.service.impl;

import com.atguigu.entity.BaseBrand;
import com.atguigu.entity.BaseCategoryView;
import com.atguigu.entity.PlatformPropertyKey;
import com.atguigu.entity.SkuInfo;
import com.atguigu.feign.SkuDetailFeignClient;
import com.atguigu.dao.ProductSearchRepository;
import com.atguigu.search.Product;
import com.atguigu.search.SearchPlatformProperty;
import com.atguigu.service.ProductSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
public class ProductSearchServiceImpl implements ProductSearchService {
    @Autowired
    private SkuDetailFeignClient skuDetailFeignClient;
    @Autowired
    private ProductSearchRepository productSearchMapper;
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;


    @Override
    public void onsale(Long skuId) {
        Product product = new Product();

        CompletableFuture<SkuInfo> skuInfoFuture = CompletableFuture.supplyAsync(() -> {
            //添加商品信息
            SkuInfo skuInfo = skuDetailFeignClient.getSkuInfo(skuId);
            product.setId(skuId);
            product.setDefaultImage(skuInfo.getSkuDefaultImg());
            product.setProductName(skuInfo.getSkuName());
            product.setPrice(skuInfo.getPrice().doubleValue());
            product.setCreateTime(new Date());

            return skuInfo;
        }, threadPoolExecutor);

        CompletableFuture<Void> brandFuture = skuInfoFuture.thenAcceptAsync(skuInfo -> {
            //添加品牌相关信息
            BaseBrand brand = skuDetailFeignClient.getBrand(skuInfo.getBrandId());
            product.setBrandId(brand.getId());
            product.setBrandName(brand.getBrandName());
            product.setBrandLogoUrl(brand.getBrandLogoUrl());
        }, threadPoolExecutor);

        CompletableFuture<Void> categoryViewFuture = skuInfoFuture.thenAcceptAsync(skuInfo -> {
            //设置分类信息
            BaseCategoryView categoryView = skuDetailFeignClient.getCategoryView(skuInfo.getCategory3Id());
            product.setCategory1Id(categoryView.getCategory1Id());
            product.setCategory1Name(categoryView.getCategory1Name());
            product.setCategory2Id(categoryView.getCategory2Id());
            product.setCategory2Name(categoryView.getCategory2Name());
            product.setCategory3Id(categoryView.getCategory3Id());
            product.setCategory3Name(categoryView.getCategory3Name());
        }, threadPoolExecutor);

        CompletableFuture<Void> platformPropertyKeyListFuture = skuInfoFuture.thenAcceptAsync(skuInfo -> {
            //设置平台属性集合对象
            List<PlatformPropertyKey> platformPropertyKeyList = skuDetailFeignClient.getPlatformPropertyBySkuId(skuId);
            List<SearchPlatformProperty> searchPlatformPropertyList = platformPropertyKeyList.stream().map(platformPropertyKey -> {
                SearchPlatformProperty searchPlatformProperty = new SearchPlatformProperty();

                searchPlatformProperty.setPropertyKeyId(platformPropertyKey.getId());
                searchPlatformProperty.setPropertyValue(platformPropertyKey.getPropertyValueList().get(0).getPropertyValue());
                searchPlatformProperty.setPropertyKey(platformPropertyKey.getPropertyKey());

                return searchPlatformProperty;
            }).collect(Collectors.toList());

            product.setPlatformProperty(searchPlatformPropertyList);
        }, threadPoolExecutor);

        CompletableFuture.allOf(skuInfoFuture, brandFuture, categoryViewFuture, platformPropertyKeyListFuture).join();

        productSearchMapper.save(product);
    }

    @Override
    public void offSale(Long skuId) {
        productSearchMapper.deleteById(skuId);
    }

    @Deprecated
    public void onsaleDeprecated(Long skuId) {
        Product product = new Product();

        //添加商品信息
        SkuInfo skuInfo = skuDetailFeignClient.getSkuInfo(skuId);
        product.setId(skuId);
        product.setDefaultImage(skuInfo.getSkuDefaultImg());
        product.setProductName(skuInfo.getSkuName());
        product.setPrice(skuInfo.getPrice().doubleValue());
        product.setCreateTime(new Date());

        //添加品牌相关信息
        BaseBrand brand = skuDetailFeignClient.getBrand(skuInfo.getBrandId());
        product.setBrandId(brand.getId());
        product.setBrandName(brand.getBrandName());
        product.setBrandLogoUrl(brand.getBrandLogoUrl());

        //设置分类信息
        BaseCategoryView categoryView = skuDetailFeignClient.getCategoryView(skuInfo.getCategory3Id());
        product.setCategory1Id(categoryView.getCategory1Id());
        product.setCategory1Name(categoryView.getCategory1Name());
        product.setCategory2Id(categoryView.getCategory2Id());
        product.setCategory2Name(categoryView.getCategory2Name());
        product.setCategory3Id(categoryView.getCategory3Id());
        product.setCategory3Name(categoryView.getCategory3Name());

        //设置平台属性集合对象
        List<PlatformPropertyKey> platformPropertyKeyList = skuDetailFeignClient.getPlatformPropertyBySkuId(skuId);
        List<SearchPlatformProperty> searchPlatformPropertyList = platformPropertyKeyList.stream().map(platformPropertyKey -> {
            SearchPlatformProperty searchPlatformProperty = new SearchPlatformProperty();

            searchPlatformProperty.setPropertyKeyId(platformPropertyKey.getId());
            searchPlatformProperty.setPropertyValue(platformPropertyKey.getPropertyValueList().get(0).getPropertyValue());
            searchPlatformProperty.setPropertyKey(platformPropertyKey.getPropertyKey());

            return searchPlatformProperty;
        }).collect(Collectors.toList());

        product.setPlatformProperty(searchPlatformPropertyList);

        productSearchMapper.save(product);
    }
}
