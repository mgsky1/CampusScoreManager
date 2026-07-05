package com.campusscore.persistence.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 学生列表 / 详情投影行（对应 contracts §4.1 item）。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentListItem {
    private Long id;
    private String loginName;
    private String realName;
    private String tel;
    private String address;
    private Integer grade;
}
