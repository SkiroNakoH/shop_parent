package com.atguigu.controller;


import com.atguigu.entity.BaseSaleProperty;
import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.entity.ProductSpu;
import com.atguigu.result.RetVal;
import com.atguigu.service.BaseSalePropertyService;
import com.atguigu.service.ProductSpuService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 商品表 前端控制器
 * </p>
 *
 * @author SkiroNakoH
 * @since 2023-05-18
 */
@RestController
@RequestMapping("/product")
public class SpuController {

    @Autowired
    private ProductSpuService productSpuService;
    @Autowired
    private BaseSalePropertyService baseSalePropertyService;

    @GetMapping("/queryProductSpuByPage/{pageNum}/{pageSize}/{categroy3Id}")
    public RetVal queryProductSpuByPage(@PathVariable Integer pageNum,
                                        @PathVariable Integer pageSize,
                                        @PathVariable Long categroy3Id) {
        Page<ProductSpu> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<ProductSpu> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProductSpu::getCategory3Id, categroy3Id);
        productSpuService.page(page, queryWrapper);

        return RetVal.ok(page);
    }

    @GetMapping("/queryAllSaleProperty")
    public RetVal queryProductSpuByPage() {
        List<BaseSaleProperty> salePropertyList = baseSalePropertyService.list(null);
        return RetVal.ok(salePropertyList);
    }

    @PostMapping("/saveProductSpu")
    public RetVal saveProductSpu(@RequestBody ProductSpu productSpu) {
        productSpuService.saveProductSpu(productSpu);
        return RetVal.ok();
    }

}

