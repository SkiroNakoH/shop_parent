package com.atguigu.aop;

import com.atguigu.constant.RedisConst;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class ShopCacheAspect {

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private RBloomFilter rBloomFilter;

    //    @Around("@annotation(com.atguigu.aop.ShopCache)")
    public Object cacheAroundAdvice1(ProceedingJoinPoint pjp) throws Throwable {
        //获取目标对象类的方法参数
        Object[] methodParams = pjp.getArgs();

        //获取目标对象类的方法
        MethodSignature methodSignature = (MethodSignature) pjp.getSignature();
        Method method = methodSignature.getMethod();
        //获取目标对象类的注解
        ShopCache annotation = method.getAnnotation(ShopCache.class);

        //获取注解中的缓存名
        String prefix = annotation.value();

        Object firstParam = methodParams[0];
        String cacheKey = prefix + ":" + firstParam;

        Object obj = redisTemplate.opsForValue().get(cacheKey);
        //判断是否要加锁
        if (obj == null) {
            //分布式锁保证线程安全
            RLock lock = redissonClient.getLock("lock-" + firstParam);
            try {
                lock.lock();
                //判断是否要从数据库中查找
                if (obj == null) {
                    //判断是否需要查找布隆过滤器
                    if (annotation.enableBloom()) {
                        //判断skuid是否在布隆过滤器中
                        if (rBloomFilter.contains(firstParam)) {
                            //从DB中取值
                            obj = pjp.proceed();
                        }
                    } else {
                        //从DB中取值
                        obj = pjp.proceed();
                    }
                }
                //存入redis
                redisTemplate.opsForValue().set(cacheKey, obj, annotation.redisTime(), TimeUnit.SECONDS);
            } finally {
                lock.unlock();
            }
        }

        return obj;
    }

    //redis缓存+布隆过滤器+双重校验+redisson锁
    //因为本环绕通知处理的是读操作吗，所以改本地锁
    @Around("@annotation(com.atguigu.aop.ShopCache)")
    public Object cacheAroundAdvice(ProceedingJoinPoint pjp) throws Throwable {
        //获取目标对象类的方法参数
        Object[] methodParams = pjp.getArgs();

        //获取目标对象类的方法
        MethodSignature methodSignature = (MethodSignature) pjp.getSignature();
        Method method = methodSignature.getMethod();
        //获取目标对象类的注解
        ShopCache annotation = method.getAnnotation(ShopCache.class);

        //获取注解中的缓存名
        String prefix = annotation.value();
        Object firstParam;

        if(methodParams != null && methodParams.length > 0){
             firstParam = ":" + methodParams[0];
        }else{
            firstParam = "";
        }
        String cacheKey = prefix + firstParam;

        Object obj = redisTemplate.opsForValue().get(cacheKey);
        //判断是否要加锁
        if (obj == null) {
            //本地锁,通过lock-id比较，
            /**
             * intern()解释:
             * 定义两个string类型的变量a,b
             * a.intern() == b 相当于 a.equals(b)
             */
            synchronized (("lock" + firstParam).intern()) {
                //判断是否要从数据库中查找
                if (obj == null) {
                    //判断是否需要查找布隆过滤器
                    if (annotation.enableBloom()) {
                        //判断skuid是否在布隆过滤器中
                        if (rBloomFilter.contains(firstParam)) {
                            //从DB中取值
                            obj = pjp.proceed();
                        }
                    } else {
                        //从DB中取值
                        obj = pjp.proceed();
                    }
                }
            }
            //存入redis
            redisTemplate.opsForValue().set(cacheKey, obj, annotation.redisTime(), TimeUnit.SECONDS);

        }

        return obj;
    }

}
