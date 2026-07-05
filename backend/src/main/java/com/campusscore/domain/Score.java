package com.campusscore.domain;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 单条成绩记录，对应 {@code score} 表。
 *
 * <p>{@code grade} 为录入时的学生学年快照。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Score {
    private Long id;
    private Long studentId;
    private Long subjectId;
    private Long teacherId;
    private Integer score;
    private Integer grade;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
