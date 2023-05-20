package com.atguigu.controller;

import com.alibaba.fastjson.JSON;
import com.atguigu.feign.SkuDetailFeignClient;
import com.atguigu.entity.BaseCategoryView;
import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.entity.SkuInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Controller
public class WebController {
    @Autowired
    private SkuDetailFeignClient skuDetailFeignClient;

    @RequestMapping("/{skuId}.html")
    public String index(@PathVariable Long skuId, Model model) {
        //1.获取商品基本信息，包括商品图片
        SkuInfo skuInfo = skuDetailFeignClient.getSkuInfo(skuId);

        if(skuInfo == null)
            return "error/index";

        Long category3Id = skuInfo.getCategory3Id();
        Long productId = skuInfo.getProductId();

        //2.获取商品分类
        BaseCategoryView categoryView = skuDetailFeignClient.getCategoryView(category3Id);
        if(categoryView == null)
            return "error/index";

        //3.获取商品价格
        BigDecimal price = skuDetailFeignClient.getPrice(skuId);
        if(price == null)
            return "error/index";

        //4.获取商品属性和商品id的映射关系
        Map salePropertyAndSkuMapping = skuDetailFeignClient.getSalePropertyAndSkuMapping(productId);
        if(salePropertyAndSkuMapping == null)
            return "error/index";

        //5.获取商品属性
        List<ProductSalePropertyKey> spuSalePropertyList = skuDetailFeignClient.getSpuSalePropertyList(productId, skuId);
        if(spuSalePropertyList == null)
            return "error/index";

        model.addAttribute("skuInfo", skuInfo);
        model.addAttribute("price", price);
        model.addAttribute("categoryView", categoryView);
        model.addAttribute("salePropertyValueIdJson", JSON.toJSONString(salePropertyAndSkuMapping));
        model.addAttribute("spuSalePropertyList", spuSalePropertyList);

        return "detail/index";
    }
}
