package com.atguigu.service;

import com.atguigu.entity.CartInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 购物车表 用户登录系统时更新冗余 服务类
 * </p>
 *
 * @author SkiroNakoH
 * @since 2023-05-30
 */
public interface CartInfoService extends IService<CartInfo> {

    void addCart(Long skuId, Integer skuNum, String oneOfUserId);

    List<CartInfo> getCartList(String userId, String userTempId);

    void checkCart(Long skuId, Integer isChecked, String oneOfUserId);

    void deleteCart(Long skuId, String oneOfUserId);

    List<CartInfo> getSelectedCartInfo(String userId);

}
