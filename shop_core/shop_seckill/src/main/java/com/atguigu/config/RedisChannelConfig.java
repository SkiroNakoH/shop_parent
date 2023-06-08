package com.atguigu.config;

import com.atguigu.constant.RedisConst;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisChannelConfig {
    //监听消息
    @Bean
    MessageListenerAdapter listenerAdapter(SecKillMsgReciver secKillMsgReciver){
        /**
         * @param delegate the delegate object
         * @param defaultListenerMethod method to call when a message comes
         */
        return new MessageListenerAdapter(secKillMsgReciver,"receiveChannelMessage");
    }


    //订阅的容器
    @Bean
    RedisMessageListenerContainer container(  RedisConnectionFactory redisConnectionFactory, MessageListenerAdapter listenerAdapter){
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        //设置连接工厂
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(listenerAdapter,new PatternTopic(RedisConst.PREPARE_PUB_SUB_SECKILL));
        return container;
    }
}
