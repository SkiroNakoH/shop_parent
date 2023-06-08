package com.atguigu.consumer;

import com.atguigu.constant.MqConst;
import com.atguigu.constant.RedisConst;
import com.atguigu.entity.SeckillProduct;
import com.atguigu.service.SeckillProductService;
import com.atguigu.utils.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
            exchange = @Exchange(MqConst.SCAN_SECKILL_EXCHANGE),
            key = MqConst.SCAN_SECKILL_ROUTE_KEY))
    public void sendMSG2ScanSeckill() {
        //todo 接收扫描秒杀商品，并上架
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
}
