package com.atguigu.fallback;


import com.atguigu.entity.BaseCategoryView;
import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.entity.SkuInfo;
import com.atguigu.feign.SkuDetailFeignClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SkuDetailFallBackService  implements SkuDetailFeignClient {
    @Override
    public SkuInfo getSkuInfo(Long skuId) {
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setSkuDesc("当前SkuInfo服务出错，请稍后重试 getSkuInfo, (┬＿┬)");
        //防止controller获取参数时报错
        skuInfo.setProductId(0L);
        skuInfo.setCategory3Id(0L);
        return skuInfo;
    }

    @Override
    public BaseCategoryView getCategoryView(Long categroy3Id) {
        BaseCategoryView baseCategoryView = new BaseCategoryView();
        baseCategoryView.setCategory1Name("当前CategoryView服务出错，请稍后重试 getCategoryView, (┬＿┬)");
        return baseCategoryView;
    }

    @Override
    public BigDecimal getPrice(Long skuId) {
        return new BigDecimal(0);
    }

    @Override
    public Map getSalePropertyAndSkuMapping(Long productId) {
        Map<Object, Object> map = new HashMap<>();
        map.put("fallback","当前Mapping服务出错，请稍后重试  getSalePropertyAndSkuMapping, (┬＿┬)");
        return map;
    }

    @Override
    public List<ProductSalePropertyKey> getSpuSalePropertyList(Long productId, Long skuId) {
        List<ProductSalePropertyKey> list = new ArrayList<>();
        ProductSalePropertyKey propertyKey = new ProductSalePropertyKey();
        propertyKey.setSalePropertyKeyName("当前PropertyList服务出错，请稍后重试 getSpuSalePropertyList, (┬＿┬)");

        list.add(propertyKey);
        return list;
    }
}
