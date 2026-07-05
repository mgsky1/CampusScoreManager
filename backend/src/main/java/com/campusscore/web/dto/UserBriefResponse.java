package com.campusscore.web.dto;

import com.campusscore.domain.Role;
import com.campusscore.service.UserBriefView;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 用户概要响应。 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserBriefResponse {
    private Long id;
    private String loginName;
    private String realName;
    private Role role;

    public static UserBriefResponse from(UserBriefView v) {
        return UserBriefResponse.builder()
                .id(v.getId())
                .loginName(v.getLoginName())
                .realName(v.getRealName())
                .role(v.getRole())
                .build();
    }
}
