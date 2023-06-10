package com.atguigu.service;

import com.atguigu.entity.SeckillProduct;
import com.atguigu.entity.UserSeckillSkuInfo;
import com.atguigu.result.RetVal;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;
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

    RetVal generateSeckillCode(Long skuId, HttpServletRequest request);

    RetVal prepareSeckill(Long skuId, String seckillCode, HttpServletRequest request);

    void prepareSeckillOrder(UserSeckillSkuInfo userSeckillSkuInfo);

    RetVal hasQualified(Long skuId, HttpServletRequest request);
}
