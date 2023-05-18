package com.atguigu.service;

import com.atguigu.entity.ProductSpu;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 商品表 服务类
 * </p>
 *
 * @author SkiroNakoH
 * @since 2023-05-18
 */
public interface ProductSpuService extends IService<ProductSpu> {

    void saveProductSpu(ProductSpu productSpu);
}
