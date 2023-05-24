package com.atguigu.threadPool;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;


@Data
@ConfigurationProperties(prefix = "thread.pool")
public class MyThreadPoolProperties {
    private int corePoolSize = 16;
    private int maximumPoolSize = 32;
    private long keepAliveTime = 30L;
    private int workQueueNum = 100;
}
