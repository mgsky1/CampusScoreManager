package com.campusscore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.campusscore.common.ErrorCode;
import com.campusscore.common.exception.AuthException;
import com.campusscore.common.exception.BusinessException;
import com.campusscore.domain.Role;
import com.campusscore.domain.User;
import com.campusscore.persistence.UserMapper;
import com.campusscore.security.JwtService;
import com.campusscore.security.JwtService.ParsedToken;
import com.campusscore.service.dto.AuthTokens;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * AuthService 单元测试 (T053)。
 *
 * <p>覆盖：
 * <ul>
 *   <li>登录成功：返回 access + refresh + user；
 *   <li>登录失败（密码错）：抛 {@code BusinessException(LOGIN_FAILED=2001)}；
 *   <li>登录失败（账号不存在）：同样抛 {@code 2001}（不透露具体原因）；
 *   <li>refresh 成功：返回新的 token 对；
 *   <li>refresh 无效（access token 冒充）：抛 {@code AuthException(1401)}；
 *   <li>refresh 无效（用户已删）：抛 {@code AuthException(1401)}。
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserMapper userMapper;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;

    @InjectMocks AuthService authService;

    private static User teacher() {
        return User.builder()
                .id(1L)
                .realName("田老师")
                .loginName("ttt")
                .passwordHash("$2a$10$hash")
                .role(Role.TEACHER)
                .build();
    }

    @Test
    void loginSuccessReturnsTokensAndUserBrief() {
        User u = teacher();
        when(userMapper.findByLoginName("ttt")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("123456", u.getPasswordHash())).thenReturn(true);
        when(jwtService.generateAccess(1L, "ttt", Role.TEACHER)).thenReturn("acc");
        when(jwtService.generateRefresh(1L, "ttt", Role.TEACHER)).thenReturn("ref");

        AuthTokens r = authService.login("ttt", "123456");

        assertThat(r.getAccessToken()).isEqualTo("acc");
        assertThat(r.getRefreshToken()).isEqualTo("ref");
        assertThat(r.getUser().getId()).isEqualTo(1L);
        assertThat(r.getUser().getLoginName()).isEqualTo("ttt");
        assertThat(r.getUser().getRole()).isEqualTo(Role.TEACHER);
    }

    @Test
    void loginWithWrongPasswordThrowsLoginFailed() {
        User u = teacher();
        when(userMapper.findByLoginName("ttt")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("bad", u.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login("ttt", "bad"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LOGIN_FAILED);
    }

    @Test
    void loginWithUnknownLoginNameThrowsSameLoginFailed() {
        when(userMapper.findByLoginName("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("ghost", "whatever"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LOGIN_FAILED);
    }

    @Test
    void refreshSuccessReturnsNewTokenPair() {
        ParsedToken parsed =
                new ParsedToken(1L, "ttt", Role.TEACHER, JwtService.TYPE_REFRESH, Instant.now());
        when(jwtService.parse("ref")).thenReturn(parsed);
        when(userMapper.findById(1L)).thenReturn(Optional.of(teacher()));
        when(jwtService.generateAccess(1L, "ttt", Role.TEACHER)).thenReturn("acc2");
        when(jwtService.generateRefresh(1L, "ttt", Role.TEACHER)).thenReturn("ref2");

        AuthTokens r = authService.refresh("ref");
        assertThat(r.getAccessToken()).isEqualTo("acc2");
        assertThat(r.getRefreshToken()).isEqualTo("ref2");
    }

    @Test
    void refreshWithAccessTokenIsRejected() {
        ParsedToken parsed =
                new ParsedToken(1L, "ttt", Role.TEACHER, JwtService.TYPE_ACCESS, Instant.now());
        when(jwtService.parse("acc")).thenReturn(parsed);

        assertThatThrownBy(() -> authService.refresh("acc"))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void refreshWhenUserGoneThrows() {
        ParsedToken parsed =
                new ParsedToken(9L, "gone", Role.STUDENT, JwtService.TYPE_REFRESH, Instant.now());
        when(jwtService.parse("ref")).thenReturn(parsed);
        when(userMapper.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("ref"))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void getBriefReturnsCurrentUser() {
        when(userMapper.findById(3L)).thenReturn(Optional.of(User.builder()
                .id(3L)
                .loginName("hhh")
                .realName("黄同学")
                .role(Role.STUDENT)
                .build()));

        UserBriefView v = authService.getBrief(3L);
        assertThat(v.getId()).isEqualTo(3L);
        assertThat(v.getLoginName()).isEqualTo("hhh");
        assertThat(v.getRealName()).isEqualTo("黄同学");
        assertThat(v.getRole()).isEqualTo(Role.STUDENT);
    }
}
