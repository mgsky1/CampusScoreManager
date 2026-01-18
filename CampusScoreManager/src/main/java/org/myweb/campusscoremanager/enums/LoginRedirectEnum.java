package org.myweb.campusscoremanager.enums;

import lombok.Getter;

public enum LoginRedirectEnum {
    TEACHER(1, "TEACHER", "教师端"),
    STUDENT(0, "STUDENT", "学生端");

    private Integer code;

    @Getter
    private String redirect;

    private String desc;

    LoginRedirectEnum(Integer code, String redirect, String desc) {
        this.code = code;
        this.redirect = redirect;
        this.desc = desc;
    }

    public static LoginRedirectEnum getByCode(Integer code) {
        for (LoginRedirectEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
