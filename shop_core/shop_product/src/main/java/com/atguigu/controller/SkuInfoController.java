package com.atguigu.controller;


import com.atguigu.entity.ProductImage;
import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.entity.SkuInfo;
import com.atguigu.result.RetVal;
import com.atguigu.service.BaseSalePropertyService;
import com.atguigu.service.ProductImageService;
import com.atguigu.service.ProductSalePropertyKeyService;
import com.atguigu.service.SkuInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 库存单元表 前端控制器
 * </p>
 *
 * @author SkiroNakoH
 * @since 2023-05-18
 */
@RestController
@RequestMapping("/product")
public class SkuInfoController {
    @Autowired
    private ProductSalePropertyKeyService propertyKeyService;
    @Autowired
    private ProductImageService productImageService;
    @Autowired
    private SkuInfoService skuInfoService;

    @GetMapping("/querySalePropertyByProductId/{productId}")
    public RetVal querySalePropertyByProductId(@PathVariable Long productId) {

        List<ProductSalePropertyKey> salePropertyKeyList =
                propertyKeyService.querySalePropertyByProductId(productId);
        return RetVal.ok(salePropertyKeyList);
    }

    @GetMapping("/queryProductImageByProductId/{productId}")
    public RetVal queryProductImageByProductId(@PathVariable Long productId) {
        LambdaQueryWrapper<ProductImage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProductImage::getProductId, productId);

        List<ProductImage> imageList = productImageService.list(queryWrapper);
        return RetVal.ok(imageList);
    }

    @PostMapping("/saveSkuInfo")
    public RetVal saveSkuInfo(@RequestBody SkuInfo skuInfo){

        skuInfoService.saveSkuInfo(skuInfo);
        return RetVal.ok();
    }
}

