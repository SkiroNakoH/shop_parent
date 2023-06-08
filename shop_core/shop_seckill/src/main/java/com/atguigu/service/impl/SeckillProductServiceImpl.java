package com.atguigu.service.impl;

import com.atguigu.constant.RedisConst;
import com.atguigu.entity.SeckillProduct;
import com.atguigu.mapper.SeckillProductMapper;
import com.atguigu.service.SeckillProductService;
import com.atguigu.utils.DateUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author SkiroNakoH
 * @since 2023-06-08
 */
@Service
public class SeckillProductServiceImpl extends ServiceImpl<SeckillProductMapper, SeckillProduct> implements SeckillProductService {
    @Autowired
    private RedisTemplate redisTemplate;
    //创建一级缓存
    Map<Long, SeckillProduct> cacheMap = new ConcurrentHashMap<>();

    @Override
    public List<SeckillProduct> queryAllSeckill() {
        //一级缓存有值
        if (cacheMap.size() > 0) {
            return cacheMap.entrySet().stream()
                    .map(Map.Entry::getValue)
                    .sorted(Comparator.comparing(SeckillProduct::getStartTime)).collect(Collectors.toList());
        }

        //缓存没值
        //从redis中查询
        Boolean flag = redisTemplate.hasKey(RedisConst.SECKILL_PRODUCT);
        if (flag) {
            List<SeckillProduct> seckillProductList = redisTemplate.boundHashOps(RedisConst.SECKILL_PRODUCT).values();

            //按开始时间排序
//        seckillProductList.stream().sorted((o1,o2)-> DateUtil.truncatedCompareTo(o1.getStartTime(),o2.getStartTime(), Calendar.SECOND)).collect(Collectors.toList());
            seckillProductList = seckillProductList.stream().sorted(Comparator.comparing(SeckillProduct::getStartTime)).collect(Collectors.toList());
            //存入缓存
            for (SeckillProduct seckillProduct : seckillProductList) {
                cacheMap.put(seckillProduct.getSkuId(), seckillProduct);
            }

            return seckillProductList;
        }
        return null;
    }

    @Override
    public SeckillProduct querySecKillBySkuId(Long skuId) {
        //查看一级缓存
        if(cacheMap.containsKey(skuId))
            return cacheMap.get(skuId);
        
        //查看缓存
        Boolean flag = redisTemplate.boundHashOps(RedisConst.SECKILL_PRODUCT).hasKey(skuId.toString());
        if (flag) {
            return (SeckillProduct) redisTemplate.boundHashOps(RedisConst.SECKILL_PRODUCT).get(skuId.toString());
        }
        return null;
    }
}
