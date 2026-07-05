package com.campusscore.web.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.Data;

/**
 * contracts §2.2 请求体。学生需 tel+address，教师仅需 tel；
 * 服务端按 role 分派：教师传的 address 会被忽略。
 * 为了简化前端，两组字段共存；address 使用 {@link Size} 而非 NotBlank，
 * 教师提交时可省略；服务端会对学生做二次强校验。
 */
@Data
public class UpdateProfileRequest {
    @NotBlank(message = "tel 不能为空")
    @Pattern(regexp = "\\d{8,11}", message = "tel 必须为 8-11 位数字")
    private String tel;

    /** 仅学生使用；教师可省略。 */
    @Size(max = 50, message = "address 长度不能超过 50")
    private String address;
}
