package com.campusscore.persistence.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 成绩列表投影行（Mapper join 结果）。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreListItem {
    private Long id;
    private Long subjectId;
    private String subjectName;
    private Long teacherId;
    private String teacherName;
    private Integer grade;
    private Integer score;
    private LocalDateTime updatedAt;
}
