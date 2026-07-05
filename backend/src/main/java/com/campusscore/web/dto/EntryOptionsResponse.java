package com.campusscore.web.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** contracts §6.2 entry-options 响应。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntryOptionsResponse {
    private StudentBriefResponse student;
    private List<EntryOptionResponse> options;
    private boolean allEntered;
}
