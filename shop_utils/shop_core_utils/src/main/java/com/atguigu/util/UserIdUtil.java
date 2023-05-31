package com.atguigu.util;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.constant.RedisConst;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;

public class UserIdUtil {


    //获取userId
    public static String getUserId(HttpServletRequest request,RedisTemplate redisTemplate) {
        String userId = null;
        String token = request.getHeader("token");
        if (!StringUtils.isEmpty(token)) {
            String userKey = RedisConst.USER_LOGIN_KEY_PREFIX + token;
            JSONObject userJsonObject = (JSONObject) redisTemplate.opsForValue().get(userKey);
            if (userJsonObject != null) {
                userId =  String.valueOf((Long)userJsonObject.get("userId"));
            }

        }
        return userId;
    }
}
