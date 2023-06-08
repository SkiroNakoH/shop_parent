package com.atguigu.config;

import com.atguigu.constant.RedisConst;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class SecKillMsgReciver {
    @Autowired
    private RedisTemplate redisTemplate;
    //一级缓存 操作内存
    public Map<String,String> secKillState= new HashMap<>();
    public void receiveChannelMessage(String message){
        //""24:1""  seckill:state:24 1
        message = message.replaceAll("\"", "");
        String[] messageSplit = message.split(":");
        if (messageSplit.length == 2) {
            //存入一级缓存
            secKillState.put(RedisConst.SECKILL_STATE_PREFIX+messageSplit[0],messageSplit[1]);
            //可以来秒杀了
            redisTemplate.opsForValue().set(RedisConst.SECKILL_STATE_PREFIX+messageSplit[0],messageSplit[1]);
        }
    }
}
