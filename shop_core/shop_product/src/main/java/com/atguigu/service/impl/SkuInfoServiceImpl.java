package com.atguigu.service.impl;

import com.atguigu.constant.RedisConst;
import com.atguigu.entity.SkuImage;
import com.atguigu.entity.SkuInfo;
import com.atguigu.entity.SkuPlatformPropertyValue;
import com.atguigu.entity.SkuSalePropertyValue;
import com.atguigu.mapper.SkuInfoMapper;
import com.atguigu.service.SkuImageService;
import com.atguigu.service.SkuInfoService;
import com.atguigu.service.SkuPlatformPropertyValueService;
import com.atguigu.service.SkuSalePropertyValueService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCommand;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 库存单元表 服务实现类
 * </p>
 *
 * @author SkiroNakoH
 * @since 2023-05-18
 */
@Service
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoMapper, SkuInfo> implements SkuInfoService {
    @Autowired
    private SkuPlatformPropertyValueService skuPlatformPropertyValueService;
    @Autowired
    private SkuSalePropertyValueService skuSalePropertyValueService;
    @Autowired
    private SkuImageService skuImageService;
    @Autowired
    private RedisTemplate redisTemplate;

    @Transactional
    @Override
    public void saveSkuInfo(SkuInfo skuInfo) {
        //保存库存单元表
        save(skuInfo);

        Long skuId = skuInfo.getId();
        Long productId = skuInfo.getProductId();

        //保存sku平台属性值关联表
        List<SkuPlatformPropertyValue> platformPropertyValueList = skuInfo.getSkuPlatformPropertyValueList();
        if (!CollectionUtils.isEmpty(platformPropertyValueList)) {
            for (SkuPlatformPropertyValue platformPropertyValue : platformPropertyValueList) {
                platformPropertyValue.setSkuId(skuId);
            }
            skuPlatformPropertyValueService.saveBatch(platformPropertyValueList);
        }

        //保存sku销售属性值
        List<SkuSalePropertyValue> salePropertyValueList = skuInfo.getSkuSalePropertyValueList();
        if (!CollectionUtils.isEmpty(salePropertyValueList)) {
            for (SkuSalePropertyValue salePropertyValue : salePropertyValueList) {
                salePropertyValue.setSkuId(skuId);
                salePropertyValue.setProductId(productId);
            }
            skuSalePropertyValueService.saveBatch(salePropertyValueList);
        }

        //保存库存单元图片表
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (!CollectionUtils.isEmpty(skuImageList)) {
            for (SkuImage skuImage : skuImageList) {
                skuImage.setSkuId(skuId);
            }
            skuImageService.saveBatch(skuImageList);
        }
    }

    @Override
    public SkuInfo getSkuInfo(Long skuId) {
        return getInfoFromRedis(skuId);
    }

    private SkuInfo getInfoFromRedis(Long skuId) {
        //拼接redis存取key
        String keyString = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;

        SkuInfo skuInfo = (SkuInfo) redisTemplate.opsForValue().get(keyString);
        if (Objects.isNull(skuInfo)) {
            //从DB中取值
            skuInfo = getInfoFromDB(skuId);

            //设置redis中key的编码方式
//            redisTemplate.setKeySerializer(StringRedisSerializer.UTF_8);

            //存入redis
            redisTemplate.opsForValue().set(keyString, skuInfo, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
        }

        return skuInfo;
    }

    private SkuInfo getInfoFromDB(Long skuId) {
        SkuInfo skuInfo = getById(skuId);

        LambdaQueryWrapper<SkuImage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SkuImage::getSkuId, skuId);
        List<SkuImage> skuImageList = skuImageService.list(queryWrapper);

        skuInfo.setSkuImageList(skuImageList);
        return skuInfo;
    }
}
