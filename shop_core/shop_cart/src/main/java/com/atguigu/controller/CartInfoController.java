package com.atguigu.controller;


import com.alibaba.fastjson.JSONObject;
import com.atguigu.constant.RedisConst;
import com.atguigu.result.RetVal;
import com.atguigu.service.CartInfoService;
import com.atguigu.util.AuthContextHolder;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * <p>
 * 购物车表 用户登录系统时更新冗余 前端控制器
 * </p>
 *
 * @author SkiroNakoH
 * @since 2023-05-30
 */
@RestController
@RequestMapping("/cart")
public class CartInfoController {
    @Autowired
    private CartInfoService cartInfoService;
    @Autowired
    private RedisTemplate redisTemplate;

    @GetMapping("/addCart/{skuId}/{skuNum}")
    public void addCart(@PathVariable Long skuId, @PathVariable Integer skuNum, HttpServletRequest request) {
        String oneOfUserId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(oneOfUserId))
            oneOfUserId = AuthContextHolder.getUserTempId(request);

        cartInfoService.addCart(skuId, skuNum, oneOfUserId);
    }

    @GetMapping("/getCartList")
    public RetVal getCartList(HttpServletRequest request) {

        String userTempId = AuthContextHolder.getUserTempId(request);
        //获取userId
        String userId = null;
        String token = request.getHeader("token");
        if (!StringUtils.isEmpty(token)) {
            String userKey = RedisConst.USER_LOGIN_KEY_PREFIX + token;
            JSONObject userJsonObject = (JSONObject) redisTemplate.opsForValue().get(userKey);
            if (userJsonObject != null) {
                userId =  String.valueOf((Long)userJsonObject.get("userId"));
            }

        }
        return RetVal.ok(cartInfoService.getCartList(userId, userTempId));
    }

}

