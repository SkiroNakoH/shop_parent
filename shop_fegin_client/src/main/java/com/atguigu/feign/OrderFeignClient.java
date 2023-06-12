package com.atguigu.feign;

import com.atguigu.entity.OrderInfo;
import com.atguigu.entity.UserAddress;
import com.atguigu.result.RetVal;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.Valid;
import java.util.List;

@FeignClient("shop-order")
public interface OrderFeignClient {
    @GetMapping("/order/confirm")
    public RetVal confirm();

    @GetMapping("/order/getOrderInfoAndOrderDetail/{orderId}")
    public OrderInfo getOrderInfoAndOrderDetail(@PathVariable Long orderId);

    @PostMapping("/order/saveOrderAndDetail")
    public Long saveOrderAndDetail(@RequestBody OrderInfo orderInfo);
}
