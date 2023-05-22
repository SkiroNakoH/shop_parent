package com.atguigu.config;

import com.atguigu.constant.RedisConst;
import com.google.common.hash.BloomFilter;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
public class BloomFilterConfig {
    @Autowired
    private RedissonClient redissonClient;

    @Bean
    public RBloomFilter getBloomFilter(){
        RBloomFilter<Object> skuBloomFilter = redissonClient.getBloomFilter(RedisConst.BLOOM_SKU_ID);
//      设置布隆容器大小和容错率
        skuBloomFilter.tryInit(10000,0.0001);
        return skuBloomFilter;
    }

}
