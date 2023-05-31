package com.atguigu.controller;


import com.alibaba.fastjson.JSONObject;
import com.atguigu.constant.RedisConst;
import com.atguigu.entity.UserAddress;
import com.atguigu.entity.UserInfo;
import com.atguigu.result.RetVal;
import com.atguigu.service.UserAddressService;
import com.atguigu.service.UserInfoService;
import com.atguigu.util.IpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 用户表 前端控制器
 * </p>
 *
 * @author SkiroNakoH
 * @since 2023-05-29
 */
@RestController
@RequestMapping("/user")
public class UserInfoController {
    @Autowired
    private UserInfoService userInfoService;
    @Autowired
    private UserAddressService userAddressService;
    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping("/login")
    public RetVal login(@RequestBody UserInfo uiUserInfo, HttpServletRequest request) {
        UserInfo userInfo = userInfoService.getUserInfoFromDb(uiUserInfo);

        //登录失败，用户名或密码不正确
        if (userInfo == null)
            return RetVal.fail().message("登录失败,用户名或密码不正确");

        //用户存在，将用户存入redis做缓存
        String token = UUID.randomUUID().toString();
        String userKey = RedisConst.USER_LOGIN_KEY_PREFIX + token;

        JSONObject userJsonObject = new JSONObject();
        userJsonObject.put("userId", userInfo.getId());
        userJsonObject.put("loginIp", IpUtil.getIpAddress(request));
        redisTemplate.opsForValue().set(userKey, userJsonObject, RedisConst.USERKEY_TIMEOUT, TimeUnit.SECONDS);

        //封装数据给前端
        Map retMap = new HashMap();
        retMap.put("nickName", userInfo.getNickName());
        retMap.put("token",token);

        return RetVal.ok(retMap);
    }

    @GetMapping("/logout")
    public RetVal logout(HttpServletRequest request){
        //从redis中删除 用户缓存
        String token = request.getHeader("token");
        String userLoginKey = RedisConst.USER_LOGIN_KEY_PREFIX + token;

        redisTemplate.delete(userLoginKey);

        return RetVal.ok();
    }

    //3.根据用户id查询用户收货地址
    @GetMapping("getUserAddressByUserId/{userId}")
    public List<UserAddress> getUserAddressByUserId(@PathVariable String userId) {
        LambdaQueryWrapper<UserAddress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAddress::getUserId, userId);
        return userAddressService.list(wrapper);
    }
}

