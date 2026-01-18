package org.myweb.campusscoremanager.result;

import lombok.Data;

/**
 * @Desc:
 * @Author: huangzhiyuan
 * @CreateDate: 2026/1/18 21:21
 * @Modify:
 */
@Data
public class LoginResult {

    private String token;

    private String redirect;
}
