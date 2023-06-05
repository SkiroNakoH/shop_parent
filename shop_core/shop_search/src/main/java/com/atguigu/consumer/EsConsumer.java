package com.atguigu.consumer;

import com.atguigu.constant.MqConst;
import com.atguigu.constant.RedisConst;
import com.atguigu.service.ProductSearchService;
import com.atguigu.util.MD5;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class EsConsumer {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private ProductSearchService productSearchService;

    @SneakyThrows
    //绑定
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = MqConst.ON_SALE_QUEUE, durable = "false"),
            exchange = @Exchange(value = MqConst.ON_OFF_SALE_EXCHANGE, durable = "false"),
            key = {MqConst.ON_SALE_ROUTING_KEY}))
    public void onSale(Long skuId, Channel channel, Message message) {

        if(skuId != null){
            productSearchService.onSale(skuId);
        }
        //产生异常，放入redis，防止多次重复消费
        String encrypt = MD5.encrypt(skuId + "");
        Long count = redisTemplate.opsForValue().increment(RedisConst.RETRY_ONSALE_KEY + encrypt);
        if(count<=RedisConst.RETRY_COUNT){
            //尝试重试
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }else{
            //todo: 短信通知管理人员
            redisTemplate.delete(RedisConst.RETRY_ONSALE_KEY + encrypt);
        }
    }

    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = MqConst.OFF_SALE_QUEUE,durable = "false"),
    exchange = @Exchange(value = MqConst.ON_OFF_SALE_EXCHANGE,durable = "false"),
    key = {MqConst.OFF_SALE_ROUTING_KEY}))
    public void offSale(Long skuId,Channel channel,Message message){
        if(skuId != null){
            productSearchService.offSale(skuId);
        }
        //判断次数，存入redis
        String encrypt = MD5.encrypt(skuId + "");
        Long count = redisTemplate.opsForValue().increment(RedisConst.RETRY_OFFSALE_KEY + encrypt);
        if(count <= RedisConst.RETRY_COUNT){
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }else {
            //todo: 短信通知管理人员
            redisTemplate.delete(RedisConst.RETRY_OFFSALE_KEY + encrypt);
        }
    }
}
