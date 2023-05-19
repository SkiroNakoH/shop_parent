package com.atguigu.service.impl;

import com.atguigu.entity.SkuSalePropertyValue;
import com.atguigu.mapper.SkuSalePropertyValueMapper;
import com.atguigu.service.SkuSalePropertyValueService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * sku销售属性值 服务实现类
 * </p>
 *
 * @author SkiroNakoH
 * @since 2023-05-18
 */
@Service
public class SkuSalePropertyValueServiceImpl extends ServiceImpl<SkuSalePropertyValueMapper, SkuSalePropertyValue> implements SkuSalePropertyValueService {

    @Autowired
    private SkuSalePropertyValueMapper skuSalePropertyValueMapper;

    @Override
    public Map getSalePropertyAndSkuMapping(Long productId) {
        //1.从数据库获取List<map>的映射关系
        List<Map> saleMappingList = skuSalePropertyValueMapper.getSalePropertyAndSkuMapping(productId);

        //2.将list改为map
        Map<Object, Object> SalePropertyAndSkuMap = new HashMap<>();

        if (!CollectionUtils.isEmpty(saleMappingList)) {
            for (Map map : saleMappingList) {
                SalePropertyAndSkuMap.put(map.get("sale_property_value"), map.get("sku_id"));
            }
        }

        return SalePropertyAndSkuMap;
    }
}
