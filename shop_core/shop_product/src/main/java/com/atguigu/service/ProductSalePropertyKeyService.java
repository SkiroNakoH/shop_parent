package com.atguigu.service;

import com.atguigu.entity.ProductSalePropertyKey;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * spu销售属性 服务类
 * </p>
 *
 * @author SkiroNakoH
 * @since 2023-05-18
 */
public interface ProductSalePropertyKeyService extends IService<ProductSalePropertyKey> {

    List<ProductSalePropertyKey> querySalePropertyByProductId(Long productId);
}
