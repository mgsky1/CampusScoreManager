package com.campusscore.web.dto;

import com.campusscore.domain.Subject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** contracts §5 单条课程 item。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectItemResponse {
    private Long id;
    private String name;
    private Integer grade;

    public static SubjectItemResponse from(Subject s) {
        return SubjectItemResponse.builder()
                .id(s.getId())
                .name(s.getName())
                .grade(s.getGrade())
                .build();
    }
}
