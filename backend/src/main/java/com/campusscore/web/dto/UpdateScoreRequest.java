package com.campusscore.web.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.Data;

/** contracts §6.4 请求体。 */
@Data
public class UpdateScoreRequest {
    @NotNull(message = "score 不能为空")
    @Min(value = 0, message = "成绩最小为 0")
    @Max(value = 100, message = "成绩最大为 100")
    private Integer score;
}
