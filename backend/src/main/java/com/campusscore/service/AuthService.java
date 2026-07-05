package com.campusscore.service;

import com.campusscore.common.ErrorCode;
import com.campusscore.common.exception.AuthException;
import com.campusscore.common.exception.BusinessException;
import com.campusscore.domain.User;
import com.campusscore.persistence.UserMapper;
import com.campusscore.security.JwtService;
import com.campusscore.security.JwtService.ParsedToken;
import com.campusscore.service.dto.AuthTokens;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务：登录、刷新、获取当前用户概要。
 *
 * <p>为避免账号枚举，登录失败一律返回 {@link ErrorCode#LOGIN_FAILED}（2001）。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /** 登录：校验凭证 → 签发 access + refresh。 */
    public AuthTokens login(String loginName, String rawPassword) {
        Optional<User> found = userMapper.findByLoginName(loginName);
        if (!found.isPresent()) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED, "登录名或密码不正确");
        }
        User u = found.get();
        if (!passwordEncoder.matches(rawPassword, u.getPasswordHash())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED, "登录名或密码不正确");
        }
        return buildTokens(u);
    }

    /** 用有效的 refresh token 换取新的 token 对。 */
    public AuthTokens refresh(String refreshToken) {
        ParsedToken parsed = jwtService.parse(refreshToken);
        if (!parsed.isRefresh()) {
            throw new AuthException("token 类型不匹配");
        }
        Optional<User> found = userMapper.findById(parsed.getUserId());
        if (!found.isPresent()) {
            throw new AuthException("用户不存在");
        }
        return buildTokens(found.get());
    }

    /** 获取用户概要。用于 {@code GET /api/v1/auth/me}。 */
    public UserBriefView getBrief(long userId) {
        User u = userMapper.findById(userId)
                .orElseThrow(() -> new AuthException("用户不存在"));
        return toBrief(u);
    }

    private AuthTokens buildTokens(User u) {
        String access = jwtService.generateAccess(u.getId(), u.getLoginName(), u.getRole());
        String refresh = jwtService.generateRefresh(u.getId(), u.getLoginName(), u.getRole());
        return AuthTokens.builder()
                .accessToken(access)
                .refreshToken(refresh)
                .user(toBrief(u))
                .build();
    }

    private UserBriefView toBrief(User u) {
        return UserBriefView.builder()
                .id(u.getId())
                .loginName(u.getLoginName())
                .realName(u.getRealName())
                .role(u.getRole())
                .build();
    }
}
