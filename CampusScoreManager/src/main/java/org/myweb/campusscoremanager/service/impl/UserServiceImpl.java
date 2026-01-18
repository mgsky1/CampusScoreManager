package org.myweb.campusscoremanager.service.impl;

import cn.acmsmu.mgsky1.shiro.util.TokenUtils;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

import org.myweb.campusscoremanager.enums.LoginRedirectEnum;
import org.myweb.campusscoremanager.mapper.UserMapper;
import org.myweb.campusscoremanager.param.LoginParam;
import org.myweb.campusscoremanager.pojo.User;
import org.myweb.campusscoremanager.result.LoginResult;
import org.myweb.campusscoremanager.service.UserControlService;
import org.myweb.campusscoremanager.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Desc:
 * @Author: huangzhiyuan
 * @CreateDate: 2026/1/18 21:02
 * @Modify:
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private UserControlService userControlService;

    @Autowired
    private UserMapper userMapper;

    @Override
    public LoginResult login(LoginParam param) {
        LoginResult result = new LoginResult();
        String token = null;
        try {
            token = userControlService.login(param.getUserName(), param.getPassword());
        } catch (Exception e) {
            log.error("登录失败...", e);
        }
        result.setToken(token);
        if (StrUtil.isBlank(token)) {
            return result;
        }
        Long userId = TokenUtils.getUserIdFromToken(token);
        User user = userMapper.selectById(userId);
        result.setRedirect(Objects
            .requireNonNull(LoginRedirectEnum.getByCode(user.getPriviledge()))
            .getRedirect());
        return result;
    }

    @Override
    public void logout() {
        userControlService.logout();
    }
}
