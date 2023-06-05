package com.atguigu.consumer;

import com.atguigu.constant.MqConst;
import com.atguigu.entity.OrderInfo;
import com.atguigu.enums.OrderStatus;
import com.atguigu.enums.ProcessStatus;
import com.atguigu.service.OrderInfoService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrderConsumer {
    @Autowired
    private OrderInfoService orderInfoService;

    @SneakyThrows
    @RabbitListener(queues = MqConst.CANCEL_ORDER_QUEUE)
    public void cancelOrder(Long orderId, Channel channel, Message message) {
        if (orderId != null) {
            OrderInfo orderInfo = orderInfoService.getById(orderId);
            orderInfo.setOrderStatus(OrderStatus.CLOSED.name());
            orderInfo.setProcessStatus(ProcessStatus.CLOSED.name());

            orderInfoService.updateById(orderInfo);
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);

    }

}
