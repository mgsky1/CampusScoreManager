package com.campusscore.web.dto;

import com.campusscore.persistence.dto.ScoreListItem;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 成绩列表条目响应体（{@code contracts/api.md §3.1}）。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreItemResponse {
    private Long id;
    private Long subjectId;
    private String subjectName;
    private Long teacherId;
    private String teacherName;
    private Integer grade;
    private Integer score;

    /** 是否不及格 (score &lt; 60)。 */
    @JsonProperty("isFailing")
    private boolean failing;

    private LocalDateTime updatedAt;

    public boolean isFailing() {
        return failing;
    }

    public static ScoreItemResponse from(ScoreListItem row) {
        int s = row.getScore() == null ? 0 : row.getScore();
        return ScoreItemResponse.builder()
                .id(row.getId())
                .subjectId(row.getSubjectId())
                .subjectName(row.getSubjectName())
                .teacherId(row.getTeacherId())
                .teacherName(row.getTeacherName())
                .grade(row.getGrade())
                .score(row.getScore())
                .failing(s < 60)
                .updatedAt(row.getUpdatedAt())
                .build();
    }
}
