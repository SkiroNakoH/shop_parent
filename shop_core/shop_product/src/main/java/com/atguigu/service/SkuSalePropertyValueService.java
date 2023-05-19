package com.atguigu.service;

import com.atguigu.entity.SkuSalePropertyValue;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

/**
 * <p>
 * sku销售属性值 服务类
 * </p>
 *
 * @author SkiroNakoH
 * @since 2023-05-18
 */
public interface SkuSalePropertyValueService extends IService<SkuSalePropertyValue> {

    Map getSalePropertyAndSkuMapping(Long productId);
}
