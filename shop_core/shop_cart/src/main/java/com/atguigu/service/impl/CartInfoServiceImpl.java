package com.atguigu.service.impl;

import com.atguigu.entity.CartInfo;
import com.atguigu.mapper.CartInfoMapper;
import com.atguigu.service.CartInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 购物车表 用户登录系统时更新冗余 服务实现类
 * </p>
 *
 * @author SkiroNakoH
 * @since 2023-05-30
 */
@Service
public class CartInfoServiceImpl extends ServiceImpl<CartInfoMapper, CartInfo> implements CartInfoService {

}
