package com.campusscore.web.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.Data;

/** contracts §6.3 请求体。 */
@Data
public class CreateScoreRequest {
    @NotNull(message = "subjectId 不能为空")
    private Long subjectId;

    @NotNull(message = "score 不能为空")
    @Min(value = 0, message = "成绩最小为 0")
    @Max(value = 100, message = "成绩最大为 100")
    private Integer score;
}
