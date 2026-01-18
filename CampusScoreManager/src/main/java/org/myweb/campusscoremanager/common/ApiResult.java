package org.myweb.campusscoremanager.common;

import lombok.Data;

import org.myweb.campusscoremanager.enums.RespCodeEnum;

/**
 * @Desc:
 * @Author: huangzhiyuan
 * @CreateDate: 2026/1/18 21:03
 * @Modify:
 */
@Data
public class ApiResult<T> {

    private Integer code = RespCodeEnum.OK.getCode();

    private String msg = "成功";

    private T data;

    public ApiResult() {

    }

    public ApiResult(T data) {
        this.data = data;
    }

    public ApiResult(Integer code, String msg, T data) {
        this.code = code;
        this.data = data;
        this.msg = msg;
    }
}
