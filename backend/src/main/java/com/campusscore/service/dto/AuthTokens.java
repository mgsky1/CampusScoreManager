package com.campusscore.service.dto;

import com.campusscore.service.UserBriefView;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** 认证结果值对象。 */
@Getter
@Builder
@AllArgsConstructor
public class AuthTokens {
    private final String accessToken;
    private final String refreshToken;
    private final UserBriefView user;
}
