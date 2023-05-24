package com.atguigu.threadPool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

@EnableConfigurationProperties(MyThreadPoolProperties.class)
@Configuration
public class MyThreadPool {
    @Autowired
    private MyThreadPoolProperties myThreadPoolProperties;

    @Bean
    public ThreadPoolExecutor getMyThreadPool() {
        return new ThreadPoolExecutor(myThreadPoolProperties.getCorePoolSize(),
                myThreadPoolProperties.getMaximumPoolSize(),
                myThreadPoolProperties.getKeepAliveTime(), TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(myThreadPoolProperties.getWorkQueueNum()),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }
}
