package com.atguigu.schedule;

import com.atguigu.constant.MqConst;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*@Component
@EnableScheduling*/
@RestController
public class SeckillSchedule {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    //每天6点执行
//    @Scheduled(cron = "0 0 6 * * ?")
    @GetMapping("/sendMsg2ScanSeckill")
    public void sendMsg2ScanSeckill(){
        //mq`通知
        rabbitTemplate.convertAndSend(MqConst.SCAN_SECKILL_EXCHANGE,MqConst.SCAN_SECKILL_ROUTE_KEY,"");
    }

    //每天2点，清理秒杀信息
//    @Scheduled(cron = "0 0 2 * * ?")
    @GetMapping("/sendMsg2ClearRedis")
    public void sendMsg2ClearRedis(){
        //mq`通知
        rabbitTemplate.convertAndSend(MqConst.CLEAR_REDIS_EXCHANGE,MqConst.CLEAR_REDIS_ROUTE_KEY,"");
    }
}
