package com.atguigu.consumer;

import com.atguigu.constant.MqConst;
import com.atguigu.constant.RedisConst;
import com.atguigu.entity.SeckillProduct;
import com.atguigu.entity.UserSeckillSkuInfo;
import com.atguigu.service.SeckillProductService;
import com.atguigu.utils.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;

@Component
public class SeckillConsumer {
    @Autowired
    private SeckillProductService seckillProductService;
    @Autowired
    private RedisTemplate redisTemplate;

    //接收扫描秒杀商品，并上架
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = MqConst.SCAN_SECKILL_QUEUE, durable = "false"),
            exchange = @Exchange(value = MqConst.SCAN_SECKILL_EXCHANGE, durable = "false"),
            key = MqConst.SCAN_SECKILL_ROUTE_KEY))
    public void scanSeckill() {
        //接收扫描秒杀商品，并上架
        /* 1. 扫描出当天应上架的商品
                状态： 能上架
                数量: >0
                时间：当天
         */
        QueryWrapper<SeckillProduct> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);
        queryWrapper.gt("num", 0);
        queryWrapper.eq("DATE_FORMAT(start_time,'%Y-%m-%d')", DateUtil.formatDate(new Date()));

        List<SeckillProduct> seckillProductList = seckillProductService.list(queryWrapper);

        if (CollectionUtils.isEmpty(seckillProductList))
            return;

        for (SeckillProduct seckillProduct : seckillProductList) {
            //2. 把查询出的秒杀商品放入缓存  hash存储
            String skuId = seckillProduct.getSkuId().toString();
            redisTemplate.boundHashOps(RedisConst.SECKILL_PRODUCT).put(skuId, seckillProduct);
            //3. 把秒杀商品的数量放入redis  list存储
            for (Integer i = 0; i < seckillProduct.getNum(); i++) {
                redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + skuId).leftPush(skuId);
            }
            //4. 当商品上架时，通知其他redis节点可以秒杀了，改变秒杀状态
            redisTemplate.convertAndSend(RedisConst.PREPARE_PUB_SUB_SECKILL, skuId + ":" + RedisConst.CAN_SECKILL);
        }
    }

    //预下单
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = MqConst.PREPARE_SECKILL_QUEUE, durable = "false"),
            exchange = @Exchange(value = MqConst.PREPARE_SECKILL_EXCHANGE, durable = "false"),
            key = MqConst.PREPARE_SECKILL_ROUTE_KEY))
    public void prepareSeckillOrder(UserSeckillSkuInfo userSeckillSkuInfo, Channel channel, Message message) {
        if (userSeckillSkuInfo != null) {
            seckillProductService.prepareSeckillOrder(userSeckillSkuInfo);
        }

    }

    //清理过期的秒杀商品
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.CLEAR_REDIS_QUEUE,durable = "false"),
            exchange = @Exchange(value = MqConst.CLEAR_REDIS_EXCHANGE,durable = "false",autoDelete = "true"),
            key = {MqConst.CLEAR_REDIS_ROUTE_KEY}
    ))
    public void clearRedis(Message message, Channel channel) throws Exception{
        QueryWrapper<SeckillProduct> wrapper = new QueryWrapper<>();
        //1为秒杀商品
        wrapper.eq("status",1);
        wrapper.le("end_time",new Date());
        //获取到秒杀结束之后的商品数据
        List<SeckillProduct> seckillProductList = seckillProductService.list(wrapper);
        for (SeckillProduct seckillProduct : seckillProductList) {
            //删除库存数
            redisTemplate.delete(RedisConst.SECKILL_STOCK_PREFIX + seckillProduct.getSkuId());
        }
        //删除秒杀商品信息
        redisTemplate.delete(RedisConst.SECKILL_PRODUCT);
        //删除用户抢得预售订单
        redisTemplate.delete(RedisConst.PREPARE_SECKILL_USERID_ORDER);
        // 删除用户秒杀最终抢到的订单
        redisTemplate.delete(RedisConst.BOUGHT_SECKILL_USER_ORDER);
        // 更新数据 更新状态 1 表示秒杀开始，2 表示秒杀结束
        SeckillProduct seckillProduct = new SeckillProduct();
        seckillProduct.setStatus("2");
        seckillProductService.update(seckillProduct,wrapper);
        // 消息确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

}
