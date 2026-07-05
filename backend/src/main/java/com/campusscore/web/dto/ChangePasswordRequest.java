package com.campusscore.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Data;

/** contracts §2.3 请求体。 */
@Data
public class ChangePasswordRequest {
    @NotBlank(message = "oldPassword 不能为空")
    private String oldPassword;

    @NotBlank(message = "newPassword 不能为空")
    @Size(min = 6, max = 32, message = "newPassword 长度需在 6-32 之间")
    private String newPassword;

    @NotBlank(message = "confirmPassword 不能为空")
    private String confirmPassword;

    /**
     * 字段级校验：{@code newPassword.equals(confirmPassword)}。
     * 若不匹配 → 400/1000（field=confirmPassword）；controller 层再复核并映射到 2003。
     */
    @AssertTrue(message = "confirmPassword 必须等于 newPassword")
    @JsonIgnore
    public boolean isConfirmMatchesNew() {
        if (newPassword == null || confirmPassword == null) return true; // 由 @NotBlank 兜底
        return newPassword.equals(confirmPassword);
    }
}
