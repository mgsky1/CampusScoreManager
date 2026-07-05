package com.campusscore.service;

import com.campusscore.domain.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** 用户概要视图，供 Service 层内部使用（Controller 再映射为 DTO）。 */
@Getter
@Builder
@AllArgsConstructor
public class UserBriefView {
    private final Long id;
    private final String loginName;
    private final String realName;
    private final Role role;
}
