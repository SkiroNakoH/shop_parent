package com.atguigu.service;

import com.atguigu.controller.SkuDetailController;
import com.atguigu.entity.SkuInfo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 库存单元表 服务类
 * </p>
 *
 * @author SkiroNakoH
 * @since 2023-05-18
 */
public interface SkuInfoService extends IService<SkuInfo> {

    void saveSkuInfo(SkuInfo skuInfo);

    SkuInfo getSkuInfo(Long skuId);

    SkuInfo getInfoFromDB(Long skuId);
}
