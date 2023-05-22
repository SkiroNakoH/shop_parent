package com.atguigu.service.impl;

import com.atguigu.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class RedisServiceImpl implements RedisService {
    @Autowired
    private RedisTemplate redisTemplate;

    //并发时，有损失
    //加锁
//    @Override
    public void setNum1() {
        doBusiness();
    }

    //当服务器建立集群时，线程不安全
    //使用分布式锁
    //    @Override
    public synchronized void setNum2() {
        doBusiness();
    }

    //当doBusiness()业务出错时，redis锁无法得到释放
    //设置redis锁的有效期
//    @Override
    public void setNum3() {
        //尝试拿锁
        boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", "ok");
        if (lock) {
            doBusiness();
            //删除锁
            redisTemplate.delete("lock");
        } else {
            //尝试再次获得锁   递归
            setNum();
        }
    }

    //由于锁的有效期与业务有效期不一致，当业务还在进行时，锁过期，会导致锁资源提前被释放，让其他业务拿到锁资源
    //加上uuid唯一标识进行识别
//    @Override
    public void setNum4() {
        //尝试拿锁
        boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", "ok", 3, TimeUnit.HOURS);
        if (lock) {
            doBusiness();
            //删除锁
            redisTemplate.delete("lock");
        } else {
            //尝试再次获得锁   递归
            setNum();
        }
    }

    //判断是否为当前锁已经删除锁有时间差，缺乏原子性
    //使用 lua 脚本
//    @Override
    public void setNum5() {
        String token = UUID.randomUUID().toString();
        //尝试拿锁
        boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", token, 3, TimeUnit.HOURS);
        if (lock) {
            doBusiness();
            //删除锁
            String redisToken = (String) redisTemplate.opsForValue().get("lock");
            if (token.equals(redisToken))
                redisTemplate.delete("lock");
        } else {
            //尝试再次获得锁   递归
            setNum();
        }
    }

    //尝试重新获取锁资源时，如果不能获得锁资源，则一直占用栈空间，并且在尝试拿锁之前的代码也会重复执行，没有意义
    //采取自旋，拿到锁资源后递归
//    @Override
    public void setNum6() {
        String token = UUID.randomUUID().toString();
        //尝试拿锁
        boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", token, 3, TimeUnit.HOURS);
        if (lock) {
            doBusiness();
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

        } else {
            //尝试再次获得锁   递归
            setNum();
        }
    }

    //当重新拿锁时，拿到的锁对象后进行递归，导致再次拿锁时，无法拿到
    //添加标识，识别是否拿到过锁
//    @Override
    public void setNum8() {
        String token = UUID.randomUUID().toString();
        //尝试拿锁
        boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", token, 3, TimeUnit.HOURS);
        if (lock) {
            doBusiness();
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

        } else {
            for (; ; ) {
                boolean retryLock = redisTemplate.opsForValue().setIfAbsent("lock", token, 3, TimeUnit.HOURS);
                if (retryLock)
                    break;
            }
            setNum();
        }
    }

    //执行lua脚本时，token对象不一样,导致锁无法被释放
    //将token做完map的value传递
    Map<Object, Boolean> map1 = new HashMap<>();
//    @Override
    public void setNum9()  {
        Boolean flag = map1.get(Thread.currentThread());
        boolean accquireLock = false;
        String token = null;

        if (flag != null && flag) {
            accquireLock = true;
        } else {
            token = UUID.randomUUID().toString();
            //尝试拿锁
            accquireLock = redisTemplate.opsForValue().setIfAbsent("lock", token, 3, TimeUnit.HOURS);
        }
        if (accquireLock) {
            doBusiness();
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

        } else {
            for (; ; ) {

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                boolean retryLock = redisTemplate.opsForValue().setIfAbsent("lock", token, 3, TimeUnit.HOURS);
                if (retryLock) {
                    map1.put(Thread.currentThread(), true);
                    break;
                }
            }
            setNum();
        }
    }

    Map<Object, String> map = new HashMap<>();
    @Override
    public void setNum()  {
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
            doBusiness();
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
            setNum();
        }
    }

    private void doBusiness() {
        String num = (String) redisTemplate.opsForValue().get("num");

        if (num == null) {
            redisTemplate.opsForValue().set("num", "1");
        } else {
            num = String.valueOf(Integer.parseInt(num) + 1);
            redisTemplate.opsForValue().set("num", num);
        }
    }

}
