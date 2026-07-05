package com.campusscore.web.dto;

import com.campusscore.service.dto.AuthTokens;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** {@code POST /api/v1/auth/login|refresh} 响应体。 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthLoginResponse {
    private String accessToken;
    private String refreshToken;
    private UserBriefResponse user;

    public static AuthLoginResponse from(AuthTokens tokens) {
        return AuthLoginResponse.builder()
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .user(UserBriefResponse.from(tokens.getUser()))
                .build();
    }
}
