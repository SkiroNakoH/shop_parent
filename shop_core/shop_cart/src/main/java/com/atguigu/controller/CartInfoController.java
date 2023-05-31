package com.atguigu.controller;


import com.alibaba.fastjson.JSONObject;
import com.atguigu.constant.RedisConst;
import com.atguigu.entity.CartInfo;
import com.atguigu.result.RetVal;
import com.atguigu.service.CartInfoService;
import com.atguigu.util.AuthContextHolder;
import com.atguigu.util.UserIdUtil;
import io.swagger.models.auth.In;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

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

    @PostMapping("/addToCart/{skuId}/{skuNum}")
    public RetVal addToCart(@PathVariable Long skuId, @PathVariable Integer skuNum, HttpServletRequest request) {
        String oneOfUserId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(oneOfUserId))
            oneOfUserId = AuthContextHolder.getUserTempId(request);

        cartInfoService.addCart(skuId, skuNum, oneOfUserId);
        return RetVal.ok();
    }

    @GetMapping("/getCartList")
    public RetVal getCartList(HttpServletRequest request) {

        String userTempId = AuthContextHolder.getUserTempId(request);
        //获取userId
        String userId = UserIdUtil.getUserId(request,redisTemplate);
        return RetVal.ok(cartInfoService.getCartList(userId, userTempId));
    }

    //购物车的勾选
    @GetMapping("/checkCart/{skuId}/{isChecked}")
    public RetVal checkCart(@PathVariable Long skuId, @PathVariable Integer isChecked, HttpServletRequest request) {
        String oneOfUserId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(oneOfUserId))
            oneOfUserId = UserIdUtil.getUserId(request,redisTemplate);

        cartInfoService.checkCart(skuId, isChecked, oneOfUserId);

        return RetVal.ok();
    }

    //购物车删除商品
    @DeleteMapping("/deleteCart/{skuId}")
    public RetVal deleteCart(@PathVariable Long skuId, HttpServletRequest request) {
        String oneOfUserId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(oneOfUserId))
            oneOfUserId = UserIdUtil.getUserId(request,redisTemplate);

        cartInfoService.deleteCart(skuId, oneOfUserId);
        return RetVal.ok();
    }


  /*  @GetMapping("getSelectedCartInfo/{userId}")
    public List<CartInfo> getSelectedCartInfo(@PathVariable String userId){
        return cartInfoService.getSelectedCartInfo(userId);
    }*/

}

