package com.atguigu.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;

@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient(){
        // 1. Create config object
        Config config = new Config();
        config.useSingleServer().setAddress("redis://192.168.21.128:6389");
       /*         .useClusterServers()
                // use "rediss://" for SSL connection
               .addNodeAddress("redis://192.168.21.128:6389");*/


// Sync and Async API
        RedissonClient redisson = Redisson.create(config);
        return redisson;
    }
}
