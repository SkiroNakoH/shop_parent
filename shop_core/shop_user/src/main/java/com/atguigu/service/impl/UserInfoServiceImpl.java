package com.atguigu.service.impl;

import com.atguigu.entity.UserInfo;
import com.atguigu.mapper.UserInfoMapper;
import com.atguigu.service.UserInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.Map;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author SkiroNakoH
 * @since 2023-05-29
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    @Override
    public UserInfo getUserInfoFromDb(UserInfo uiUserInfo) {
        LambdaQueryWrapper<UserInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserInfo::getLoginName,uiUserInfo.getLoginName());
        //对密码进行md5加密
        String passwd = uiUserInfo.getPasswd();
        String encodePasswd = DigestUtils.md5DigestAsHex(passwd.getBytes());
        queryWrapper.eq(UserInfo::getPasswd,encodePasswd);

        return  getOne(queryWrapper);
    }
}
