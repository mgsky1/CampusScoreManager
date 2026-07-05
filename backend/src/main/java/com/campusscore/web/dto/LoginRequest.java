package com.campusscore.web.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Data;

/** {@code POST /api/v1/auth/login} 请求体。 */
@Data
public class LoginRequest {

    @NotBlank(message = "登录名不能为空")
    @Size(max = 32, message = "登录名最长 32 字符")
    private String loginName;

    @NotBlank(message = "密码不能为空")
    @Size(max = 64, message = "密码最长 64 字符")
    private String password;
}
