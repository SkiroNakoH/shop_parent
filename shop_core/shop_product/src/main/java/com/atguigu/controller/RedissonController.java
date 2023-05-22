package com.atguigu.controller;

import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RequestMapping("/product")
@RestController
public class RedissonController {
    @Autowired
    private RedissonClient redissonClient;

    @GetMapping("/getRedisson")
    public String getRedisson() {
        return redissonClient.toString();
    }

    //分布锁
    @GetMapping("/lock")
    public String lock() throws InterruptedException {
        RLock lock = redissonClient.getLock("lock");
        String uuid = UUID.randomUUID().toString();
        try {
            lock.lock();
            Thread.sleep(5000);
        } finally {
            lock.unlock();
        }
        return Thread.currentThread().getName() + "执行业务" + uuid;
    }

    //读写锁
    String uuid = null;

    @GetMapping("/read")
    public String read() {
        RReadWriteLock lock = redissonClient.getReadWriteLock("rwlock");
        RLock rLock = lock.readLock();
        try {
            rLock.lock();
        } finally {
            rLock.unlock();
        }
        return uuid;
    }

    @GetMapping("/write")
    public String write() throws InterruptedException {
        RReadWriteLock lock = redissonClient.getReadWriteLock("rwlock");
        RLock rLock = lock.writeLock();
        try {
            rLock.lock();
            uuid = UUID.randomUUID().toString();
            Thread.sleep(5000);
        } finally {
            rLock.unlock();
        }
        return Thread.currentThread().getName() + "执行业务" + uuid;
    }

    //信号量Semaphore
    @GetMapping("/driverIn")
    public String driverIn() throws InterruptedException {
        RSemaphore park = redissonClient.getSemaphore("park");
        park.acquire(1);

        return Thread.currentThread().getName() + "找到车位";
    }

    @GetMapping("/driverLeft")
    public String driverLeft() {
        RSemaphore park = redissonClient.getSemaphore("park");
        park.release(1);

        return Thread.currentThread().getName() + "驶离车位";
    }

    //闭锁CountDownLatch
    @GetMapping("/commonLeft")
    public String commonLeft() throws InterruptedException {
        RCountDownLatch countDownLatch = redissonClient.getCountDownLatch("class");

        Thread.sleep(2000);
        countDownLatch.countDown();

        return Thread.currentThread().getName() + "学员离开";
    }

    @GetMapping("/teacherLeft")
    public String teacherLeft() throws InterruptedException {
        RCountDownLatch countDownLatch = redissonClient.getCountDownLatch("class");
        countDownLatch.trySetCount(6);
        countDownLatch.await();
        return Thread.currentThread().getName() + "教师离开";
    }


    //公平锁与非公平锁
    @GetMapping("/unfair/{id}")
    public String unfair(@PathVariable Long id) throws InterruptedException {
        RLock unfair = redissonClient.getLock("unfair");
        unfair.lock();
//        Thread.sleep(3000);
        System.out.println("非公平锁 " + id);
        unfair.unlock();
        return "非公平锁 " + id;
    }

    @GetMapping("/fair/{id}")
    public String fair(@PathVariable Long id) throws InterruptedException {
        RLock fair = redissonClient.getFairLock("fair");
        fair.lock();
//        Thread.sleep(3000);
        System.out.println("公平锁 " + id);
        fair.unlock();
        return "公平锁 " + id;
    }
}
