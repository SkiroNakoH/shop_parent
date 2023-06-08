package com.atguigu.service;

import com.atguigu.entity.SeckillProduct;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author SkiroNakoH
 * @since 2023-06-08
 */
public interface SeckillProductService extends IService<SeckillProduct> {

    List<SeckillProduct> queryAllSeckill();

    SeckillProduct querySecKillBySkuId(Long skuId);
}
