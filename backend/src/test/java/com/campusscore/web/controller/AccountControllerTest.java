package com.campusscore.web.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusscore.common.ErrorCode;
import com.campusscore.common.exception.BusinessException;
import com.campusscore.domain.Role;
import com.campusscore.persistence.ScoreMapper;
import com.campusscore.persistence.StudentMapper;
import com.campusscore.persistence.SubjectMapper;
import com.campusscore.persistence.TeacherMapper;
import com.campusscore.persistence.UserMapper;
import com.campusscore.security.AppUserDetails;
import com.campusscore.security.JwtService;
import com.campusscore.service.AccountService;
import com.campusscore.web.advice.GlobalExceptionHandler;
import com.campusscore.web.dto.ChangePasswordRequest;
import com.campusscore.web.dto.ProfileResponse;
import com.campusscore.web.dto.UpdateProfileRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** AccountController Web slice 测试 (T152)。 */
@WebMvcTest(controllers = AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = {
        "campusscore.jwt.secret=test-secret-please-change-me-please-change-me",
        "campusscore.jwt.access-ttl-minutes=30",
        "campusscore.jwt.refresh-ttl-days=7",
        "campusscore.jwt.issuer=test"
})
class AccountControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AccountService accountService;
    @MockBean UserMapper userMapper;
    @MockBean StudentMapper studentMapper;
    @MockBean TeacherMapper teacherMapper;
    @MockBean SubjectMapper subjectMapper;
    @MockBean ScoreMapper scoreMapper;
    @MockBean JwtService jwtService;

    private void authAs(long id, Role role) {
        AppUserDetails p = new AppUserDetails(id, "u" + id, "x", role);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(p, "x", p.getAuthorities()));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    // ---------- 2.1 GET /profile ----------

    @Test
    void profileReturnsStudentFields() throws Exception {
        authAs(3L, Role.STUDENT);
        when(accountService.getMyProfile(3L)).thenReturn(
                ProfileResponse.builder()
                        .id(3L).loginName("zs").realName("张三").role(Role.STUDENT)
                        .tel("13800138000").address("SMU").grade(2).build());

        mockMvc.perform(get("/api/v1/account/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", equalTo(0)))
                .andExpect(jsonPath("$.data.role", equalTo("STUDENT")))
                .andExpect(jsonPath("$.data.tel", equalTo("13800138000")))
                .andExpect(jsonPath("$.data.address", equalTo("SMU")))
                .andExpect(jsonPath("$.data.grade", equalTo(2)));
    }

    @Test
    void profileReturnsTeacherFieldsOnly() throws Exception {
        authAs(1L, Role.TEACHER);
        when(accountService.getMyProfile(1L)).thenReturn(
                ProfileResponse.builder()
                        .id(1L).loginName("ttt").realName("田老师").role(Role.TEACHER)
                        .tel("18065853353").build());

        mockMvc.perform(get("/api/v1/account/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", equalTo(0)))
                .andExpect(jsonPath("$.data.role", equalTo("TEACHER")))
                .andExpect(jsonPath("$.data.tel", equalTo("18065853353")))
                .andExpect(jsonPath("$.data.address").doesNotExist())
                .andExpect(jsonPath("$.data.grade").doesNotExist());
    }

    // ---------- 2.2 POST /profile/update ----------

    @Test
    void updateProfileReturns200OnSuccess() throws Exception {
        authAs(1L, Role.TEACHER);
        when(accountService.updateMyProfile(eq(1L), any(UpdateProfileRequest.class)))
                .thenReturn(ProfileResponse.builder()
                        .id(1L).loginName("ttt").realName("田老师").role(Role.TEACHER)
                        .tel("13900139000").build());

        Map<String, Object> body = new HashMap<>();
        body.put("tel", "13900139000");
        mockMvc.perform(post("/api/v1/account/profile/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", equalTo(0)))
                .andExpect(jsonPath("$.data.tel", equalTo("13900139000")));
    }

    @Test
    void updateProfileReturns400WhenTelTooShort() throws Exception {
        authAs(3L, Role.STUDENT);
        Map<String, Object> body = new HashMap<>();
        body.put("tel", "1234567"); // 7 位
        body.put("address", "SMU");

        mockMvc.perform(post("/api/v1/account/profile/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo(1000)))
                .andExpect(jsonPath("$.errors", notNullValue()));

        verify(accountService, never()).updateMyProfile(any(Long.class), any());
    }

    @Test
    void updateProfileReturns400WhenTelTooLong() throws Exception {
        authAs(3L, Role.STUDENT);
        Map<String, Object> body = new HashMap<>();
        body.put("tel", "123456789012"); // 12 位
        body.put("address", "SMU");

        mockMvc.perform(post("/api/v1/account/profile/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo(1000)));
    }

    // ---------- 2.3 POST /password/change ----------

    @Test
    void changePasswordReturns200OnSuccess() throws Exception {
        authAs(3L, Role.STUDENT);
        Map<String, Object> body = new HashMap<>();
        body.put("oldPassword", "old");
        body.put("newPassword", "newpass1");
        body.put("confirmPassword", "newpass1");

        mockMvc.perform(post("/api/v1/account/password/change")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", equalTo(0)));

        verify(accountService).changeMyPassword(eq(3L), any(ChangePasswordRequest.class));
    }

    @Test
    void changePasswordReturns400_2002WhenOldMismatch() throws Exception {
        authAs(3L, Role.STUDENT);
        doThrow(new BusinessException(ErrorCode.OLD_PASSWORD_MISMATCH, "原密码错误"))
                .when(accountService).changeMyPassword(eq(3L), any(ChangePasswordRequest.class));

        Map<String, Object> body = new HashMap<>();
        body.put("oldPassword", "wrong");
        body.put("newPassword", "newpass1");
        body.put("confirmPassword", "newpass1");

        mockMvc.perform(post("/api/v1/account/password/change")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo(2002)));
    }

    @Test
    void changePasswordReturns400_1000WhenConfirmMismatch() throws Exception {
        // 前端提交时 confirm != new → @AssertTrue 触发 → 1000（field 校验）
        authAs(3L, Role.STUDENT);
        Map<String, Object> body = new HashMap<>();
        body.put("oldPassword", "old");
        body.put("newPassword", "newpass1");
        body.put("confirmPassword", "different");

        mockMvc.perform(post("/api/v1/account/password/change")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo(1000)));

        verify(accountService, never()).changeMyPassword(any(Long.class), any());
    }

    @Test
    void changePasswordReturns400WhenNewTooShort() throws Exception {
        authAs(3L, Role.STUDENT);
        Map<String, Object> body = new HashMap<>();
        body.put("oldPassword", "old");
        body.put("newPassword", "ab");   // < 6
        body.put("confirmPassword", "ab");

        mockMvc.perform(post("/api/v1/account/password/change")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo(1000)));
    }
}
