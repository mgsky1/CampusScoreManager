package com.campusscore.web.dto;

import com.campusscore.persistence.dto.TeacherScoreItem;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 教师视角成绩条目响应体（contracts §6.1）。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherScoreItemResponse {
    private Long id;
    private Long subjectId;
    private String subjectName;
    private Integer grade;
    private Integer score;

    @JsonProperty("isFailing")
    private boolean failing;

    private boolean editable;
    private LocalDateTime updatedAt;

    public boolean isFailing() {
        return failing;
    }

    public static TeacherScoreItemResponse from(TeacherScoreItem row) {
        int s = row.getScore() == null ? 0 : row.getScore();
        return TeacherScoreItemResponse.builder()
                .id(row.getId())
                .subjectId(row.getSubjectId())
                .subjectName(row.getSubjectName())
                .grade(row.getGrade())
                .score(row.getScore())
                .failing(s < 60)
                .editable(row.isEditable())
                .updatedAt(row.getUpdatedAt())
                .build();
    }
}
