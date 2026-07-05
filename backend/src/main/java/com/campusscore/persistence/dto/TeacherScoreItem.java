package com.campusscore.persistence.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 教师视角的成绩列表条目（含 editable 计算列）。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherScoreItem {
    private Long id;
    private Long subjectId;
    private String subjectName;
    private Integer grade;
    private Integer score;
    private boolean editable;
    private LocalDateTime updatedAt;
}
