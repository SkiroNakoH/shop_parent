package com.atguigu.consumer;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.constant.MqConst;
import com.atguigu.entity.OrderInfo;
import com.atguigu.enums.OrderStatus;
import com.atguigu.enums.ProcessStatus;
import com.atguigu.service.OrderInfoService;
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

import java.util.Map;

@Component
public class OrderConsumer {
    @Autowired
    private OrderInfoService orderInfoService;

    //超时未支付，取消订单
    @SneakyThrows
    @RabbitListener(queues = MqConst.CANCEL_ORDER_QUEUE)
    public void cancelOrder(Long orderId, Channel channel, Message message) {
        if (orderId != null) {
            OrderInfo orderInfo = orderInfoService.getById(orderId);
            if (OrderStatus.UNPAID.name().equals(orderInfo.getOrderStatus())) {
                orderInfo.setOrderStatus(OrderStatus.CLOSED.name());
                orderInfo.setProcessStatus(ProcessStatus.CLOSED.name());

                orderInfoService.updateById(orderInfo);

                //todo 支付表状态改为关闭，
                //todo  如果支付宝里有交易记录，通知支付宝关闭此交易
            }
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    //支付完成，修改订单状态
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = MqConst.PAY_ORDER_QUEUE, durable = "false"),
            exchange = @Exchange(value = MqConst.PAY_ORDER_EXCHANGE, durable = "false"),
            key = {MqConst.PAY_ORDER_ROUTE_KEY}))
    public void updateOrderFromPayment(Long orderId, Channel channel, Message message) {
        if (orderId != null) {
            OrderInfo orderInfo = orderInfoService.getOrderInfoAndOrderDetail(orderId);
            if (orderInfo != null && OrderStatus.UNPAID.name().equals(orderInfo.getOrderStatus())) {

                //修改订单状态
                orderInfoService.updateOrderStatus(orderInfo,ProcessStatus.PAID);

                // 通知仓库系统，修改商品库存
                orderInfoService.sendOrderInfo2Ware4UpdateStock(orderInfo);

            }
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    //接收库存系统尝试减库存后发送的队列消息，判断是否超卖
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = MqConst.SUCCESS_DECREASE_STOCK_QUEUE, durable = "false"),
            exchange = @Exchange(value = MqConst.SUCCESS_DECREASE_STOCK_EXCHANGE, durable = "false"),
            key = {MqConst.SUCCESS_DECREASE_STOCK_ROUTE_KEY}))
    public void updateOrderStatusFromWare(String mapJson, Channel channel, Message message) {
        if (StringUtils.isEmpty(mapJson)) {
            return;
        }

        Map<String, Object> map = JSONObject.parseObject(mapJson, Map.class);
        Long orderId = Long.valueOf((String) map.get("orderId"));
        String status = (String) map.get("status");
        OrderInfo orderInfo = orderInfoService.getOrderInfoAndOrderDetail(orderId);
        //更改库存状态
        if("DEDUCTED".equals(status)){
           orderInfoService.updateOrderStatus(orderInfo,ProcessStatus.WAITING_DELEVER);
        }else {
           orderInfoService.updateOrderStatus(orderInfo,ProcessStatus.STOCK_EXCEPTION);
        }

        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
