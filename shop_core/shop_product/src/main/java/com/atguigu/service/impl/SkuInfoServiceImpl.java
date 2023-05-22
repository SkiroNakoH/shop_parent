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
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
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

    Map<Object, String> map = new HashMap<>();
    public SkuInfo getInfoFromSafeRedis(Long skuId)  {
        String token = map.get(Thread.currentThread());
        boolean accquireLock = false;

        if (StringUtils.isEmpty(token)) {
            accquireLock = true;
        } else {
            token = UUID.randomUUID().toString();
            //尝试拿锁
            accquireLock = redisTemplate.opsForValue().setIfAbsent("lock", token, 3, TimeUnit.HOURS);
        }
        if (accquireLock) {
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


            //删除锁
            String luaScript = "if " +
                    "redis.call('get', KEYS[1]) == ARGV[1] " +
                    "then " +
                    "return redis.call('del', KEYS[1]) " +
                    "else " +
                    "return 0 end";
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            //把脚本放到redisScript中
            redisScript.setScriptText(luaScript);
            //设置脚本返回数据类型
            redisScript.setResultType(Long.class);
            redisTemplate.execute(redisScript, Arrays.asList("lock"), token);

            return skuInfo;
        } else {
            for (; ; ) {

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                boolean retryLock = redisTemplate.opsForValue().setIfAbsent("lock", token, 3, TimeUnit.HOURS);
                if (retryLock) {
                    map.put(Thread.currentThread(), token);
                    break;
                }
            }
            return getInfoFromSafeRedis(skuId);
        }
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
