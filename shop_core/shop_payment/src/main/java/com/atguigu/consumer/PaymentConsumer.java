package com.atguigu.consumer;

import com.atguigu.constant.MqConst;
import com.atguigu.enums.PaymentStatus;
import com.atguigu.service.PaymentInfoService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PaymentConsumer {
    @Autowired
    private PaymentInfoService paymentInfoService;

    //修改订单状态为关闭订单
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = MqConst.CLOSE_PAYMENT_QUEUE, durable = "false"),
            exchange = @Exchange(value = MqConst.CLOSE_PAYMENT_EXCHANGE, durable = "false"),
            key = MqConst.CLOSE_PAYMENT_ROUTE_KEY))
    public void closePayment(String outTradeNo, Channel channel, Message message) {
        if (!StringUtils.isEmpty(outTradeNo)) {
            //关闭订单
            paymentInfoService.updatePaymentStatus(outTradeNo, PaymentStatus.ClOSED);
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);

    }

}
