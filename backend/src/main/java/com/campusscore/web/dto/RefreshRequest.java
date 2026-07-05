package com.campusscore.web.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

/** {@code POST /api/v1/auth/refresh} 请求体。 */
@Data
public class RefreshRequest {

    @NotBlank(message = "refreshToken 不能为空")
    private String refreshToken;
}
