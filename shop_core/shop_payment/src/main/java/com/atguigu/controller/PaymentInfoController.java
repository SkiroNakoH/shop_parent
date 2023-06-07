package com.atguigu.controller;


import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.atguigu.config.AlipayConfig;
import com.atguigu.service.PaymentInfoService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * <p>
 * 支付信息表 前端控制器
 * </p>
 *
 * @author SkiroNakoH
 * @since 2023-06-05
 */
@RestController
@RequestMapping("/payment")
public class PaymentInfoController {
    @Autowired
    private PaymentInfoService paymentInfoService;

    //    /payment/createQrCode/{orderId}
    @GetMapping("/createQrCode/{orderId}")
    public String createQrCode(@PathVariable Long orderId) {
        return paymentInfoService.createQrCode(orderId);
    }

    // 异步回调地址  内网穿透  http://3yrmq4.natappfree.cc/payment/async/notify
    @SneakyThrows
    @PostMapping("/async/notify")
    //上线 支付宝用
    public String asyncNotify(@RequestParam Map<String, String> alipayParam) {
//      JSONObject.toJSONString(alipayParam)
        //测试开发用
//    public String asyncNotify(@RequestBody Map<String,String> alipayParam) {

        //支付成功，验证签名 防止被第三方拦截修改
//        boolean signVerified = AlipaySignature.rsaCheckV1(paramsMap, ALIPAY_PUBLIC_KEY, CHARSET, SIGN_TYPE) //调用SDK验证签名
        boolean signVerified = AlipaySignature.rsaCheckV1(alipayParam, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type); //调用SDK验证签名
        if (signVerified) {
            //todo 根据trade_status 判断是否需要执行业务
            // 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            //修改支付状态、订单状态、库存数量、会员积分等...
            paymentInfoService.updatePayment(alipayParam);
            return "sucess";
        } else {
            //  验签失败则记录异常日志，并在response中返回failure.
            return "failure";
        }
//            return "failure";

    }

    //退款接口
    @GetMapping("/refund/{orderId}")
    public Boolean refund(@PathVariable Long orderId) {
        return paymentInfoService.refund(orderId);
    }

    //查询支付宝中是否有交易记录
    @GetMapping("/queryAlipayTrade/{orderId}")
    public Boolean queryAlipayTrade(@PathVariable Long orderId) {
        return paymentInfoService.queryAlipayTrade(orderId);
    }

    //    关闭交易
    @GetMapping("/closeAlipayTrade/{orderId}")
    public Boolean closeAlipayTrade(@PathVariable Long orderId) {
        return paymentInfoService.closeAlipayTrade(orderId);
    }
}

