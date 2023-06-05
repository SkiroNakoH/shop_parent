package com.atguigu.service;

import com.atguigu.entity.PaymentInfo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 支付信息表 服务类
 * </p>
 *
 * @author SkiroNakoH
 * @since 2023-06-05
 */
public interface PaymentInfoService extends IService<PaymentInfo> {

    String createQrCode(Long orderId);
}
