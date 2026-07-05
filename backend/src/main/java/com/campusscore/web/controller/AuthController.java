package com.campusscore.web.controller;

import com.campusscore.common.ApiResponse;
import com.campusscore.security.AppUserDetails;
import com.campusscore.service.AuthService;
import com.campusscore.service.dto.AuthTokens;
import com.campusscore.web.dto.AuthLoginResponse;
import com.campusscore.web.dto.LoginRequest;
import com.campusscore.web.dto.RefreshRequest;
import com.campusscore.web.dto.UserBriefResponse;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证与会话相关 API。
 *
 * <p>路径：{@code /api/v1/auth}；见 {@code contracts/api.md §1}。
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 登录：签发 access + refresh。 */
    @PostMapping("/login")
    public ApiResponse<AuthLoginResponse> login(@Valid @RequestBody LoginRequest req) {
        AuthTokens t = authService.login(req.getLoginName(), req.getPassword());
        return ApiResponse.ok(AuthLoginResponse.from(t));
    }

    /** 用有效 refresh token 换新 token 对。 */
    @PostMapping("/refresh")
    public ApiResponse<AuthLoginResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        AuthTokens t = authService.refresh(req.getRefreshToken());
        return ApiResponse.ok(AuthLoginResponse.from(t));
    }

    /** 无状态登出：仅返回 200；客户端自己清 token。 */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.ok();
    }

    /** 获取当前用户概要。 */
    @GetMapping("/me")
    public ApiResponse<UserBriefResponse> me(@AuthenticationPrincipal AppUserDetails principal) {
        return ApiResponse.ok(UserBriefResponse.from(authService.getBrief(principal.getId())));
    }
}
