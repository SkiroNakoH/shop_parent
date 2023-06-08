package com.atguigu.consumer;

import com.atguigu.constant.MqConst;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class SeckillConsumer {
    //接收扫描秒杀商品，并上架
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = MqConst.SCAN_SECKILL_QUEUE, durable = "false"),
            exchange = @Exchange(MqConst.SCAN_SECKILL_EXCHANGE),
            key = MqConst.SCAN_SECKILL_ROUTE_KEY))
    public void sendMSG2ScanSeckill(){
        //todo 接收扫描秒杀商品，并上架

    }
}
