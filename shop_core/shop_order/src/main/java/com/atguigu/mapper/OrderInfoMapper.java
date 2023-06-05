package com.atguigu.mapper;

import com.atguigu.entity.OrderInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 订单表 订单表 Mapper 接口
 * </p>
 *
 * @author SkiroNakoH
 * @since 2023-05-31
 */
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {

    OrderInfo getOrderInfoAndOrderDetail(@Param("orderId") Long orderId);
}
