package com.atguigu.service.impl;

import com.atguigu.entity.ProductImage;
import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.entity.ProductSalePropertyValue;
import com.atguigu.entity.ProductSpu;
import com.atguigu.mapper.ProductSpuMapper;
import com.atguigu.service.ProductImageService;
import com.atguigu.service.ProductSalePropertyKeyService;
import com.atguigu.service.ProductSalePropertyValueService;
import com.atguigu.service.ProductSpuService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * <p>
 * 商品表 服务实现类
 * </p>
 *
 * @author SkiroNakoH
 * @since 2023-05-18
 */
@Service
public class ProductSpuServiceImpl extends ServiceImpl<ProductSpuMapper, ProductSpu> implements ProductSpuService {
    @Autowired
    private ProductSalePropertyKeyService propertyKeyService;
    @Autowired
    private ProductSalePropertyValueService propertyValueService;
    @Autowired
    private ProductImageService productImageService;

    @Transactional
    @Override
    public void saveProductSpu(ProductSpu productSpu) {
        //保存商品主信息spu
        save(productSpu);

        Long spuId = productSpu.getId();

        //保存商品图片
        List<ProductImage> productImageList = productSpu.getProductImageList();
        if (!CollectionUtils.isEmpty(productImageList)) {
            for (ProductImage productImage : productImageList) {
                productImage.setProductId(spuId);
            }
            productImageService.saveBatch(productImageList);
        }

        //保存销售商品的key
        List<ProductSalePropertyKey> salePropertyKeyList = productSpu.getSalePropertyKeyList();
        if (!CollectionUtils.isEmpty(salePropertyKeyList)) {
            for (ProductSalePropertyKey propertyKey : salePropertyKeyList) {
                propertyKey.setProductId(spuId);

                //保存销售商品的值
                List<ProductSalePropertyValue> salePropertyValueList = propertyKey.getSalePropertyValueList();
                if (!CollectionUtils.isEmpty(salePropertyValueList)) {
                    for (ProductSalePropertyValue propertyValue : salePropertyValueList) {
                        propertyValue.setProductId(spuId);
                        propertyValue.setSalePropertyKeyName(propertyKey.getSalePropertyKeyName());
                    }
                    propertyValueService.saveBatch(salePropertyValueList);
                }

            }
            propertyKeyService.saveBatch(salePropertyKeyList);
        }
    }
}
