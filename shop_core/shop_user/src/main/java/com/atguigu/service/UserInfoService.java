package com.atguigu.service;

import com.atguigu.entity.UserInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

/**
 * <p>
 * 用户表 服务类
 * </p>
 *
 * @author SkiroNakoH
 * @since 2023-05-29
 */
public interface UserInfoService extends IService<UserInfo> {


    UserInfo getUserInfoFromDb(UserInfo uiUserInfo);
}
