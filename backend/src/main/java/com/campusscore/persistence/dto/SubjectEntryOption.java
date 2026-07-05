package com.campusscore.persistence.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 6.2 entry-options 里的可录入课程条目。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectEntryOption {
    private Long subjectId;
    private String subjectName;
    private Integer grade;
}
