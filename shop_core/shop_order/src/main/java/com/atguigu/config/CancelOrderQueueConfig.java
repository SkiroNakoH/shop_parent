package com.atguigu.config;

import com.atguigu.constant.MqConst;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CancelOrderQueueConfig {
    @Bean
    public Queue cancelOrderQueue() {
        return new Queue(MqConst.CANCEL_ORDER_QUEUE, false);
    }

    @Bean
    public CustomExchange cancelOrderExchange() {
        Map<String, Object> arguments = new HashMap<>();
        //使用延迟交换机
        arguments.put("x-delayed-type", "direct");
        return new CustomExchange(MqConst.CANCEL_ORDER_EXCHANGE, "x-delayed-message", false, true, arguments);
    }

    //绑定
    @Bean
    public Binding bindingDelayedQueue(@Qualifier("cancelOrderQueue") Queue queue,
                                       @Qualifier("cancelOrderExchange") CustomExchange customExchange) {
        return BindingBuilder.bind(queue).to(customExchange).with(MqConst.CANCEL_ORDER_ROUTE_KEY).noargs();
    }
}
