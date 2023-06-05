package com.atguigu.service;

import com.atguigu.entity.OrderInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 订单表 订单表 服务类
 * </p>
 *
 * @author SkiroNakoH
 * @since 2023-05-31
 */
public interface OrderInfoService extends IService<OrderInfo> {

    StringBuilder checkPriceAndStock(OrderInfo orderInfo);

    Long saveOrderInfo(OrderInfo orderInfo, String userId);

    OrderInfo getOrderInfoAndOrderDetail(Long orderId);
}
