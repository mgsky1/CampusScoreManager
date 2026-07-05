package com.campusscore.web.dto;

import com.campusscore.domain.Role;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** contracts §2.1 profile 响应（学生 / 教师共用一个 DTO，教师字段 grade/address 为 null）。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfileResponse {
    private Long id;
    private String loginName;
    private String realName;
    private Role role;
    private String tel;
    private String address;   // 仅学生
    private Integer grade;    // 仅学生
}
