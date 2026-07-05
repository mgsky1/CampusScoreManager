package com.campusscore.web.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;

/** contracts §5.2 请求体。 */
@Data
public class CreateSubjectRequest {
    @NotBlank(message = "name 不能为空")
    @Size(max = 50, message = "name 长度不能超过 50")
    private String name;

    @NotNull(message = "grade 不能为空")
    @Min(value = 1, message = "grade 最小为 1")
    @Max(value = 6, message = "grade 最大为 6")
    private Integer grade;
}
