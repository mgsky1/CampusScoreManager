package com.campusscore.web.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.Data;

/** contracts §4.4 请求体。password 可选（未提供则不重置）。 */
@Data
public class UpdateStudentRequest {
    @NotBlank(message = "loginName 不能为空")
    @Size(max = 50, message = "loginName 长度不能超过 50")
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "loginName 只能包含字母、数字与下划线")
    private String loginName;

    @NotBlank(message = "realName 不能为空")
    @Size(max = 20, message = "realName 长度不能超过 20")
    private String realName;

    /** 可选：若非空则重置密码。 */
    @Size(min = 6, max = 32, message = "password 长度需在 6-32 之间")
    private String password;

    @NotBlank(message = "tel 不能为空")
    @Pattern(regexp = "\\d{8,11}", message = "tel 必须为 8-11 位数字")
    private String tel;

    @NotBlank(message = "address 不能为空")
    @Size(max = 50, message = "address 长度不能超过 50")
    private String address;

    @NotNull(message = "grade 不能为空")
    @Min(value = 1, message = "grade 最小为 1")
    @Max(value = 6, message = "grade 最大为 6")
    private Integer grade;
}
