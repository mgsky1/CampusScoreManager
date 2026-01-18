package org.myweb.campusscoremanager.service;

import cn.acmsmu.mgsky1.shiro.model.BaseUserModel;
import cn.acmsmu.mgsky1.shiro.template.UserLoginTemplate;
import cn.hutool.core.util.StrUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.myweb.campusscoremanager.mapper.UserMapper;
import org.myweb.campusscoremanager.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

/**
 * @Desc:
 * @Author: huangzhiyuan
 * @CreateDate: 2026/1/18 21:09
 * @Modify:
 */
@Service
public class UserControlService extends UserLoginTemplate {

    @Autowired
    private UserMapper userMapper;
    @Override
    public Boolean verifyUser(Long userId) {
        return true;
    }

    @Override
    public BaseUserModel getUserBaseInfoByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        BaseUserModel baseUserModel = new BaseUserModel();
        baseUserModel.setUserId(userId);
        baseUserModel.setUserName(user.getLoginName());
        baseUserModel.setPassword(user.getPassword());
        return baseUserModel;
    }

    @Override
    public List<String> getUserPermissionsByUserId(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(String.valueOf(user.getPriviledge()));
    }

    @Override
    public BaseUserModel getUserBaseInfoByUserName(String userName) {
        if (StrUtil.isBlank(userName)) {
            return null;
        }
        QueryWrapper<User> qw = new QueryWrapper<>();
        qw.eq(User.LOGIN_NAME, userName);
        User user = userMapper.selectOne(qw);
        if (user == null) {
            return null;
        }
        BaseUserModel baseUserModel = new BaseUserModel();
        baseUserModel.setUserId(Long.valueOf(user.getId()));
        baseUserModel.setUserName(user.getLoginName());
        baseUserModel.setPassword(user.getPassword());
        return baseUserModel;
    }
}
