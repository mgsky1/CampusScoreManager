package org.myweb.campusscoremanager.controller;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;

import org.myweb.campusscoremanager.common.ApiResult;
import org.myweb.campusscoremanager.enums.RespCodeEnum;
import org.myweb.campusscoremanager.param.LoginParam;
import org.myweb.campusscoremanager.result.LoginResult;
import org.myweb.campusscoremanager.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Desc:
 * @Author: huangzhiyuan
 * @CreateDate: 2026/1/18 21:21
 * @Modify:
 */
@RestController
@RequestMapping("/api/v1/user/")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ApiResult<LoginResult> login(@RequestBody LoginParam param) {
        Assert.notNull(param, "参数不能为空");
        Assert.notBlank(param.getUserName(), "用户名不能为空");
        Assert.notBlank(param.getPassword(), "密码不能为空");
        LoginResult loginResult = userService.login(param);
        if (StrUtil.isBlank(loginResult.getToken())) {
            return new ApiResult<>(RespCodeEnum.LOGIN_ERROR.getCode(), RespCodeEnum.LOGIN_ERROR.getDesc(), loginResult);
        }
        return new ApiResult<>(loginResult);
    }

    @PostMapping("/logout")
    public ApiResult logout() {
        userService.logout();
        return new ApiResult();
    }
}
