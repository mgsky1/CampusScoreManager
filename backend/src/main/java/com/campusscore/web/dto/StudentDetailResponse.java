package com.campusscore.web.dto;

import com.campusscore.persistence.dto.StudentListItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 学生完整档案响应（对应 contracts/api.md §4.1 单条 item 结构）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDetailResponse {
    private Long id;
    private String loginName;
    private String realName;
    private String tel;
    private String address;
    private Integer grade;

    public static StudentDetailResponse from(StudentListItem s) {
        return StudentDetailResponse.builder()
                .id(s.getId())
                .loginName(s.getLoginName())
                .realName(s.getRealName())
                .tel(s.getTel())
                .address(s.getAddress())
                .grade(s.getGrade())
                .build();
    }
}
