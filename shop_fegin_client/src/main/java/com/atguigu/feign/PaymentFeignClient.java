package com.atguigu.feign;

import com.atguigu.entity.CartInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient("shop-payment")
public interface PaymentFeignClient {
    //退款接口
    @GetMapping("/payment/refund/{orderId}")
    public Boolean refund(@PathVariable Long orderId) ;

    //查询支付宝中是否有交易记录
    @GetMapping("/payment/queryAlipayTrade/{orderId}")
    public Boolean queryAlipayTrade(@PathVariable Long orderId);

    //    关闭交易
    @GetMapping("/payment/closeAlipayTrade/{orderId}")
    public Boolean closeAlipayTrade(@PathVariable Long orderId) ;
}