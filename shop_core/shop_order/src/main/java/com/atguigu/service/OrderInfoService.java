package com.atguigu.service;

import com.atguigu.entity.OrderInfo;
import com.atguigu.enums.ProcessStatus;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

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

    void sendOrderInfo2Ware4UpdateStock(OrderInfo orderInfo);

    void updateOrderStatus(OrderInfo orderInfo, ProcessStatus paid);

    String splitOrder(Map<String, String> map);
}
