package com.campusscore.web.dto;

import com.campusscore.persistence.dto.SubjectEntryOption;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** entry-options 的候选课程条目。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntryOptionResponse {
    private Long subjectId;
    private String subjectName;
    private Integer grade;

    public static EntryOptionResponse from(SubjectEntryOption o) {
        return EntryOptionResponse.builder()
                .subjectId(o.getSubjectId())
                .subjectName(o.getSubjectName())
                .grade(o.getGrade())
                .build();
    }
}
