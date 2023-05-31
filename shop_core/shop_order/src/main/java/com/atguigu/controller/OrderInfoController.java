package com.atguigu.controller;


import com.atguigu.constant.RedisConst;
import com.atguigu.entity.CartInfo;
import com.atguigu.entity.OrderDetail;
import com.atguigu.entity.UserAddress;
import com.atguigu.feign.CartFeignClient;
import com.atguigu.feign.UserFeignClient;
import com.atguigu.result.RetVal;
import com.atguigu.util.AuthContextHolder;
import com.atguigu.util.UserIdUtil;
import io.swagger.models.auth.In;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.*;

/**
 * <p>
 * 订单表 订单表 前端控制器
 * </p>
 *
 * @author SkiroNakoH
 * @since 2023-05-31
 */
@RestController
@RequestMapping("/order")
public class OrderInfoController {
    @Autowired
    private CartFeignClient cartFeignClient;
    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private RedisTemplate redisTemplate;

    @GetMapping("/confirm")
    public RetVal confirm(HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>();

//        String userId = UserIdUtil.getUserId(request, redisTemplate);
        String userId = AuthContextHolder.getUserId(request);

        List<UserAddress> userAddressList = userFeignClient.getUserAddressByUserId(userId);

        List<CartInfo> selectedCartInfo = cartFeignClient.getSelectedCartInfo(userId);
        ArrayList<OrderDetail> detailArrayList = new ArrayList<>();
        BigDecimal totalMoney = new BigDecimal("0");
        Integer totalNum = new Integer(0);
        //封装数据
        if (!CollectionUtils.isEmpty(selectedCartInfo)) {
            for (CartInfo cartInfo : selectedCartInfo) {
                OrderDetail orderDetail = new OrderDetail();
                orderDetail.setSkuId(cartInfo.getSkuId());
                orderDetail.setSkuName(cartInfo.getSkuName());
                orderDetail.setImgUrl(cartInfo.getImgUrl());
                orderDetail.setSkuNum(cartInfo.getSkuNum() + "");

                //订单的价格 购物车里面的实时价格
                orderDetail.setOrderPrice(cartInfo.getRealTimePrice());
                //计算总金额
                totalMoney = totalMoney.add(cartInfo.getRealTimePrice().multiply(new BigDecimal(cartInfo.getSkuNum())));
                totalNum += cartInfo.getSkuNum();

                detailArrayList.add(orderDetail);
            }

        }

        map.put("userAddressList", userAddressList);
        map.put("detailArrayList", detailArrayList);
        map.put("totalNum", totalNum);
        map.put("totalMoney",totalMoney);
        //为了防止用户重复提交，需要设置 流水号
        String tradeNo= UUID.randomUUID().toString();
        redisTemplate.opsForValue().set("user:"+userId+":tradeNo",tradeNo);

        //将流水号 返回给前端
        map.put("tradeNo",tradeNo);

        return RetVal.ok(map);
    }
}

