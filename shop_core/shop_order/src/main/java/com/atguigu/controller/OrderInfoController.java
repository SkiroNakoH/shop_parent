package com.atguigu.controller;


import com.atguigu.constant.RedisConst;
import com.atguigu.entity.CartInfo;
import com.atguigu.entity.OrderDetail;
import com.atguigu.entity.OrderInfo;
import com.atguigu.entity.UserAddress;
import com.atguigu.feign.CartFeignClient;
import com.atguigu.feign.UserFeignClient;
import com.atguigu.result.RetVal;
import com.atguigu.service.OrderInfoService;
import com.atguigu.util.AuthContextHolder;
import com.atguigu.util.UserIdUtil;
import io.swagger.models.auth.In;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
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
    private OrderInfoService orderInfoService;
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
        map.put("totalMoney", totalMoney);
        //为了防止用户重复提交，需要设置 流水号
        String tradeNo = UUID.randomUUID().toString();
        String redisTradeNoKey = "user:" + userId + ":tradeNo";
        redisTemplate.opsForValue().set(redisTradeNoKey, tradeNo);

        //将流水号 返回给前端
        map.put("tradeNo", tradeNo);

        return RetVal.ok(map);
    }

    @PostMapping("/submitOrder")
    public RetVal submitOrder(@RequestBody @Valid OrderInfo orderInfo, HttpServletRequest request) {
        String tradeNo = request.getParameter("tradeNo");
        String userId = UserIdUtil.getUserId(request, redisTemplate);

        //从redis中获取到流水号，判断是否一致
        String redisTradeNoKey = "user:" + userId + ":tradeNo";

        String redisTradeNo = (String) redisTemplate.opsForValue().get(redisTradeNoKey);
        if (!tradeNo.equals(redisTradeNo))
            return RetVal.fail().message("不能重复提交订单");

        //核对订单中商品价格和库存
        StringBuilder checkOrderMessage = orderInfoService.checkPriceAndStock(orderInfo);
        if (!StringUtils.isEmpty(checkOrderMessage.toString())) {
            return RetVal.fail().message(checkOrderMessage.append("，需要刷新页面").toString());
        }

        //保存订单
        Long orderId = orderInfoService.saveOrderInfo(orderInfo,userId);

        //删除redis中的流水号
        redisTemplate.delete(redisTradeNoKey);

        return RetVal.ok(orderId);
    }

    @PostMapping("mySubmitOrder")
    public void mySubmitOrder(@Valid OrderInfo orderInfo){
        System.out.println(orderInfo);
    }

}

