package com.atguigu.mapper;

import com.atguigu.entity.TOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author SkiroNakoH
 * @since 2023-06-05
 */
public interface TOrderMapper extends BaseMapper<TOrder> {

    List<TOrder> queryOrderAndDetail(@Param("userId") Long userId, @Param("orderId") Long orderId);
}
