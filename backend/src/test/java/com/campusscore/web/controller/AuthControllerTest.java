package com.campusscore.web.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusscore.common.ErrorCode;
import com.campusscore.common.exception.AuthException;
import com.campusscore.common.exception.BusinessException;
import com.campusscore.domain.Role;
import com.campusscore.persistence.ScoreMapper;
import com.campusscore.persistence.TeacherMapper;
import com.campusscore.persistence.UserMapper;
import com.campusscore.security.AppUserDetails;
import com.campusscore.security.JwtService;
import com.campusscore.service.AuthService;
import com.campusscore.service.UserBriefView;
import com.campusscore.service.dto.AuthTokens;
import com.campusscore.web.advice.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** AuthController Web slice 测试 (T050)。 */
@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "campusscore.jwt.secret=test-secret-please-change-me-please-change-me",
        "campusscore.jwt.access-ttl-minutes=30",
        "campusscore.jwt.refresh-ttl-days=7",
        "campusscore.jwt.issuer=test"
})
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper om;

    @MockBean AuthService authService;
    @MockBean UserMapper userMapper;
    @MockBean TeacherMapper teacherMapper;
    @MockBean ScoreMapper scoreMapper;
    @MockBean com.campusscore.persistence.StudentMapper studentMapper;
    @MockBean com.campusscore.persistence.SubjectMapper subjectMapper;
    @MockBean JwtService jwtService;

    private static AuthTokens tokens() {
        return AuthTokens.builder()
                .accessToken("acc")
                .refreshToken("ref")
                .user(UserBriefView.builder()
                        .id(1L)
                        .loginName("ttt")
                        .realName("田老师")
                        .role(Role.TEACHER)
                        .build())
                .build();
    }

    private String toJson(Map<String, Object> map) throws Exception {
        return om.writeValueAsString(map);
    }

    @Test
    void loginSuccess200() throws Exception {
        when(authService.login("ttt", "123456")).thenReturn(tokens());
        Map<String, Object> body = new HashMap<>();
        body.put("loginName", "ttt");
        body.put("password", "123456");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(toJson(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", equalTo(0)))
                .andExpect(jsonPath("$.data.accessToken", equalTo("acc")))
                .andExpect(jsonPath("$.data.refreshToken", equalTo("ref")))
                .andExpect(jsonPath("$.data.user.loginName", equalTo("ttt")))
                .andExpect(jsonPath("$.data.user.role", equalTo("TEACHER")));
    }

    @Test
    void loginWrongPassword2001() throws Exception {
        when(authService.login(eq("ttt"), anyString()))
                .thenThrow(new BusinessException(ErrorCode.LOGIN_FAILED, "登录名或密码不正确"));
        Map<String, Object> body = new HashMap<>();
        body.put("loginName", "ttt");
        body.put("password", "bad");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(toJson(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo(2001)));
    }

    @Test
    void loginEmptyLoginNameValidation1000() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("loginName", "");
        body.put("password", "123456");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(toJson(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo(1000)));
    }

    @Test
    void refreshSuccess200() throws Exception {
        when(authService.refresh("ref")).thenReturn(tokens());
        Map<String, Object> body = new HashMap<>();
        body.put("refreshToken", "ref");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType("application/json")
                        .content(toJson(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", equalTo(0)))
                .andExpect(jsonPath("$.data.accessToken", equalTo("acc")));
    }

    @Test
    void refreshExpired1401() throws Exception {
        when(authService.refresh(anyString())).thenThrow(new AuthException("token 已过期"));
        Map<String, Object> body = new HashMap<>();
        body.put("refreshToken", "expired");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType("application/json")
                        .content(toJson(body)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", equalTo(1401)));
    }

    @Test
    void logoutReturnsOk() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", equalTo(0)));
    }

    @Test
    void meReturnsUserBrief() throws Exception {
        when(authService.getBrief(3L)).thenReturn(UserBriefView.builder()
                .id(3L)
                .loginName("hhh")
                .realName("黄同学")
                .role(Role.STUDENT)
                .build());

        AppUserDetails principal = new AppUserDetails(3L, "hhh", "x", Role.STUDENT);
        org.springframework.security.core.context.SecurityContextHolder.getContext()
                .setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        principal, "x", principal.getAuthorities()));
        try {
            mockMvc.perform(get("/api/v1/auth/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id", equalTo(3)))
                    .andExpect(jsonPath("$.data.loginName", equalTo("hhh")))
                    .andExpect(jsonPath("$.data.role", equalTo("STUDENT")));
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    // Import here to keep it near usage
    private static org.springframework.test.web.servlet.request.RequestPostProcessor user(AppUserDetails ud) {
        return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(ud);
    }
}
