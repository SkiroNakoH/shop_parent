package com.atguigu.aop;

import com.atguigu.constant.RedisConst;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ShopCache {
    //缓存名称
    String value() default "";

    //存入redis的时间
    long redisTime();

    //是否开启布隆过滤器
    boolean enableBloom() default false;


}
