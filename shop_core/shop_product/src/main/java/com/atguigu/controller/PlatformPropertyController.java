package com.atguigu.controller;


import com.atguigu.entity.PlatformPropertyKey;
import com.atguigu.entity.PlatformPropertyValue;
import com.atguigu.result.RetVal;
import com.atguigu.service.PlatformPropertyKeyService;
import com.atguigu.service.PlatformPropertyValueService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 属性表 前端控制器
 * </p>
 *
 * @author SkiroNakoH
 * @since 2023-05-15
 */
//@CrossOrigin
@RestController
@Api(tags = "商品平台属性接口")
@RequestMapping("/product")
public class PlatformPropertyController {
    @Autowired
    private PlatformPropertyKeyService platformPropertyKeyService;
    @Autowired
    private PlatformPropertyValueService platformPropertyValueService;

    @ApiOperation("根据分类Id获取平台属性key和value集合")
    @GetMapping("/getPlatformPropertyByCategoryId/{category1Id}/{category2Id}/{category3Id}")
    public RetVal getPlatformPropertyByCategoryId(@PathVariable Long category1Id,
                                                  @PathVariable Long category2Id,
                                                  @PathVariable Long category3Id) {
        List<PlatformPropertyKey> platformPropertyKeyList = platformPropertyKeyService.getPlatformPropertyByCategoryId(category1Id, category2Id, category3Id);

        return RetVal.ok(platformPropertyKeyList);
    }

    @ApiOperation("根据平台属性key获取value集合")
    @GetMapping("/getPropertyValueByPropertyKeyId/{propertyKeyId}")
    public RetVal getPropertyValueByPropertyKeyId(@PathVariable Long propertyKeyId) {
        LambdaQueryWrapper<PlatformPropertyValue> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PlatformPropertyValue::getPropertyKeyId, propertyKeyId);

        List<PlatformPropertyValue> propertyValueList = platformPropertyValueService.list(queryWrapper);
        return RetVal.ok(propertyValueList);
    }

    @ApiOperation("保存/修改平台属性key与value")
    @PostMapping("/savePlatformProperty")
    public RetVal savePlatformProperty(@RequestBody PlatformPropertyKey platformPropertyKey) {
        platformPropertyKeyService.savePlatformProperty(platformPropertyKey);

        return RetVal.ok();
    }

    //feign调用获取平台属性
    @GetMapping("/getPlatformPropertyBySkuId/{skuId}")
    public List<PlatformPropertyKey> getPlatformPropertyBySkuId(@PathVariable Long skuId){
        return platformPropertyKeyService.getPlatformPropertyBySkuId(skuId);
    }
}

