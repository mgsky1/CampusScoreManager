package org.myweb.campusscoremanager.service;

import org.myweb.campusscoremanager.param.LoginParam;
import org.myweb.campusscoremanager.result.LoginResult;

/**
 * @desc:
 * @author: huangzhiyuan
 * @createDate: 2026/1/18 21:02
 * @modify:
 */
public interface UserService {

    public LoginResult login(LoginParam param);

    public void logout();
}
