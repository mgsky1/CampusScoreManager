package com.campusscore.domain;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 授课记录（教师 × 课程 × 年级），对应 {@code subject} 表。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subject {
    private Long id;
    private String name;
    private Long teacherId;
    private Integer grade;
    private LocalDateTime createdAt;
}
