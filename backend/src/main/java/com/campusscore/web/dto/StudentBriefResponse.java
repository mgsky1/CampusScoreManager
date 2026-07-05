package com.campusscore.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 学生极简概要（用于 entry-options / 教师端 header 展示）。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentBriefResponse {
    private Long id;
    private String realName;
    private Integer grade;
}
