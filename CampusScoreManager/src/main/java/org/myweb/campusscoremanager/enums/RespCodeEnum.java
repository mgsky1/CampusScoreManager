package org.myweb.campusscoremanager.enums;

import lombok.Getter;

/**
 * @Desc:
 * @Author: huangzhiyuan
 * @CreateDate: 2026/1/18 21:04
 * @Modify:
 */
public enum RespCodeEnum {
    OK(200, "成功"),
    LOGIN_ERROR(201, "用户名或密码错误");
    @Getter
    private Integer code;

    @Getter
    private String desc;

    RespCodeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

}
