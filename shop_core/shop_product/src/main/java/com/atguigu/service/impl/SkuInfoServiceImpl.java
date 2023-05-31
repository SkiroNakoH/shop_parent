package com.atguigu.service.impl;

import com.atguigu.aop.ShopCache;
import com.atguigu.constant.RedisConst;
import com.atguigu.entity.SkuImage;
import com.atguigu.entity.SkuInfo;
import com.atguigu.entity.SkuPlatformPropertyValue;
import com.atguigu.entity.SkuSalePropertyValue;
import com.atguigu.exception.SleepUtils;
import com.atguigu.mapper.SkuInfoMapper;
import com.atguigu.service.SkuImageService;
import com.atguigu.service.SkuInfoService;
import com.atguigu.service.SkuPlatformPropertyValueService;
import com.atguigu.service.SkuSalePropertyValueService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
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
    @ShopCache(value = "skuInfo", redisTime = RedisConst.SKUKEY_TIMEOUT, enableBloom = true)
    public SkuInfo getSkuInfo(Long skuId) {
        return getInfoFromDB(skuId);
    }

    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private RBloomFilter skuBloomFilter;

    public SkuInfo getInfoFromRedisson(Long skuId) {
        //拼接redis存取key
        String keyString = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;

        SkuInfo skuInfo = (SkuInfo) redisTemplate.opsForValue().get(keyString);
        //判断是否要从数据库中查找
        if (Objects.isNull(skuInfo)) {
            //判断skuid是否在布隆过滤器中
            if (skuBloomFilter.contains(skuId)) {
                //分布式锁保证线程安全
                RLock lock = redissonClient.getLock("lock-" + skuId);
                try {
                    lock.lock();
                    //从DB中取值
                    skuInfo = getInfoFromDB(skuId);

                    //存入redis
                    redisTemplate.opsForValue().set(keyString, skuInfo, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                } finally {
                    lock.unlock();
                }
            }
        }

        return skuInfo;
    }


    ThreadLocal<String> threadLocal = new ThreadLocal();

    public SkuInfo getInfoFromSafeRedis(Long skuId) {

        //拼接redis存取key
        String keyString = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;

        SkuInfo skuInfo = (SkuInfo) redisTemplate.opsForValue().get(keyString);
        if (Objects.isNull(skuInfo)) {
            String token = threadLocal.get();
            boolean accquireLock = false;

            if (StringUtils.isEmpty(token)) {
                accquireLock = true;
            } else {
                token = UUID.randomUUID().toString();
                //尝试拿锁
                accquireLock = redisTemplate.opsForValue().setIfAbsent("lock", token, 3, TimeUnit.SECONDS);
            }
            if (accquireLock) {
                //给锁续期
                Thread thread = new Thread(() -> {
                    for (; ; ) {
                        SleepUtils.sleep(3);
                        redisTemplate.expire("lock", 10, TimeUnit.SECONDS);
                        System.out.println("续期成功");
                    }
                });
                thread.setDaemon(true);
                thread.start();

                //从DB中取值
                skuInfo = getInfoFromDB(skuId);

                //存入redis
                redisTemplate.opsForValue().set(keyString, skuInfo, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);


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
                threadLocal.remove();

                return skuInfo;
            } else {
                for (; ; ) {

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    boolean retryLock = redisTemplate.opsForValue().setIfAbsent("lock", token, 3, TimeUnit.SECONDS);
                    if (retryLock) {
                        threadLocal.set(token);
                        break;
                    }
                }
                return getInfoFromSafeRedis(skuId);
            }
        }
        return skuInfo;

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

    @Override
    public SkuInfo getInfoFromDB(Long skuId) {
        SkuInfo skuInfo = getById(skuId);

        LambdaQueryWrapper<SkuImage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SkuImage::getSkuId, skuId);
        List<SkuImage> skuImageList = skuImageService.list(queryWrapper);

        skuInfo.setSkuImageList(skuImageList);
        return skuInfo;
    }
}
