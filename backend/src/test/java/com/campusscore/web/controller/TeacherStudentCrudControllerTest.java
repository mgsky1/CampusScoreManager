package com.campusscore.web.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusscore.common.ErrorCode;
import com.campusscore.common.exception.BusinessException;
import com.campusscore.common.exception.NotFoundException;
import com.campusscore.domain.Role;
import com.campusscore.persistence.ScoreMapper;
import com.campusscore.persistence.StudentMapper;
import com.campusscore.persistence.SubjectMapper;
import com.campusscore.persistence.TeacherMapper;
import com.campusscore.persistence.UserMapper;
import com.campusscore.persistence.dto.StudentListItem;
import com.campusscore.security.AppUserDetails;
import com.campusscore.security.JwtService;
import com.campusscore.service.TeacherStudentService;
import com.campusscore.web.advice.GlobalExceptionHandler;
import com.campusscore.web.dto.CreateStudentRequest;
import com.campusscore.web.dto.UpdateStudentRequest;
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

/** TeacherStudentController CRUD Web slice 测试 (T114)。覆盖 contracts §4.3 / §4.4 / §4.5。 */
@WebMvcTest(controllers = TeacherStudentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = {
        "campusscore.jwt.secret=test-secret-please-change-me-please-change-me",
        "campusscore.jwt.access-ttl-minutes=30",
        "campusscore.jwt.refresh-ttl-days=7",
        "campusscore.jwt.issuer=test"
})
class TeacherStudentCrudControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean TeacherStudentService teacherStudentService;
    @MockBean UserMapper userMapper;
    @MockBean StudentMapper studentMapper;
    @MockBean TeacherMapper teacherMapper;
    @MockBean SubjectMapper subjectMapper;
    @MockBean ScoreMapper scoreMapper;
    @MockBean JwtService jwtService;

    private void authAsTeacher(long id) {
        AppUserDetails p = new AppUserDetails(id, "ttt", "x", Role.TEACHER);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(p, "x", p.getAuthorities()));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private StudentListItem stu(long id, String loginName, String realName, int grade) {
        return StudentListItem.builder()
                .id(id).loginName(loginName).realName(realName)
                .tel("13800138000").address("SMU").grade(grade).build();
    }

    private Map<String, Object> validCreatePayload() {
        Map<String, Object> body = new HashMap<>();
        body.put("loginName", "new_stu");
        body.put("realName", "新同学");
        body.put("password", "123456");
        body.put("tel", "13800138000");
        body.put("address", "SMU");
        body.put("grade", 3);
        return body;
    }

    private Map<String, Object> validUpdatePayload() {
        Map<String, Object> body = new HashMap<>();
        body.put("loginName", "updated_stu");
        body.put("realName", "改名后");
        // password 可选
        body.put("tel", "13900139000");
        body.put("address", "SMU B");
        body.put("grade", 4);
        return body;
    }

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    // ---------- 4.3 POST /teacher/students ----------

    @Test
    void createReturns201WithStudentDetail() throws Exception {
        authAsTeacher(1L);
        when(teacherStudentService.create(any(CreateStudentRequest.class)))
                .thenReturn(stu(10L, "new_stu", "新同学", 3));

        mockMvc.perform(post("/api/v1/teacher/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validCreatePayload())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code", equalTo(0)))
                .andExpect(jsonPath("$.data.id", equalTo(10)))
                .andExpect(jsonPath("$.data.loginName", equalTo("new_stu")))
                .andExpect(jsonPath("$.data.realName", equalTo("新同学")))
                .andExpect(jsonPath("$.data.grade", equalTo(3)));
    }

    @Test
    void createReturns400WhenLoginNameBlank() throws Exception {
        authAsTeacher(1L);
        Map<String, Object> body = validCreatePayload();
        body.put("loginName", "");

        mockMvc.perform(post("/api/v1/teacher/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo(1000)))
                .andExpect(jsonPath("$.errors", notNullValue()));

        verify(teacherStudentService, never()).create(any());
    }

    @Test
    void createReturns400WhenLoginNamePatternInvalid() throws Exception {
        authAsTeacher(1L);
        Map<String, Object> body = validCreatePayload();
        body.put("loginName", "bad name!");

        mockMvc.perform(post("/api/v1/teacher/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo(1000)));
    }

    @Test
    void createReturns400WhenTelTooShort() throws Exception {
        authAsTeacher(1L);
        Map<String, Object> body = validCreatePayload();
        body.put("tel", "1234567"); // 7 位

        mockMvc.perform(post("/api/v1/teacher/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo(1000)));
    }

    @Test
    void createReturns400WhenTelTooLong() throws Exception {
        authAsTeacher(1L);
        Map<String, Object> body = validCreatePayload();
        body.put("tel", "123456789012"); // 12 位

        mockMvc.perform(post("/api/v1/teacher/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo(1000)));
    }

    @Test
    void createReturns400WhenGradeOutOfRange() throws Exception {
        authAsTeacher(1L);
        Map<String, Object> body = validCreatePayload();
        body.put("grade", 9);

        mockMvc.perform(post("/api/v1/teacher/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo(1000)));
    }

    @Test
    void createReturns400WhenPasswordTooShort() throws Exception {
        authAsTeacher(1L);
        Map<String, Object> body = validCreatePayload();
        body.put("password", "abc"); // < 6

        mockMvc.perform(post("/api/v1/teacher/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo(1000)));
    }

    @Test
    void createReturns409WhenLoginNameTaken() throws Exception {
        authAsTeacher(1L);
        when(teacherStudentService.create(any(CreateStudentRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.LOGIN_NAME_TAKEN, "登录名已被使用"));

        mockMvc.perform(post("/api/v1/teacher/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validCreatePayload())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", equalTo(2101)))
                .andExpect(jsonPath("$.message", equalTo("登录名已被使用")));
    }

    // ---------- 4.4 POST /teacher/students/{id}/update ----------

    @Test
    void updateReturns200WithNewDetail() throws Exception {
        authAsTeacher(1L);
        when(teacherStudentService.update(eq(3L), any(UpdateStudentRequest.class)))
                .thenReturn(stu(3L, "updated_stu", "改名后", 4));

        mockMvc.perform(post("/api/v1/teacher/students/{id}/update", 3L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validUpdatePayload())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", equalTo(0)))
                .andExpect(jsonPath("$.data.id", equalTo(3)))
                .andExpect(jsonPath("$.data.loginName", equalTo("updated_stu")))
                .andExpect(jsonPath("$.data.grade", equalTo(4)));
    }

    @Test
    void updateAllowsOmittedPassword() throws Exception {
        authAsTeacher(1L);
        when(teacherStudentService.update(eq(3L), any(UpdateStudentRequest.class)))
                .thenReturn(stu(3L, "updated_stu", "改名后", 4));

        Map<String, Object> body = validUpdatePayload(); // 无 password 字段
        mockMvc.perform(post("/api/v1/teacher/students/{id}/update", 3L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", equalTo(0)));
    }

    @Test
    void updateReturns400WhenPasswordProvidedButTooShort() throws Exception {
        authAsTeacher(1L);
        Map<String, Object> body = validUpdatePayload();
        body.put("password", "12"); // 提供了但长度不合规

        mockMvc.perform(post("/api/v1/teacher/students/{id}/update", 3L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo(1000)));

        verify(teacherStudentService, never()).update(anyLong(), any());
    }

    @Test
    void updateReturns400WhenRealNameBlank() throws Exception {
        authAsTeacher(1L);
        Map<String, Object> body = validUpdatePayload();
        body.put("realName", "");

        mockMvc.perform(post("/api/v1/teacher/students/{id}/update", 3L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo(1000)));
    }

    @Test
    void updateReturns404WhenStudentMissing() throws Exception {
        authAsTeacher(1L);
        when(teacherStudentService.update(eq(99L), any(UpdateStudentRequest.class)))
                .thenThrow(new NotFoundException("学生不存在"));

        mockMvc.perform(post("/api/v1/teacher/students/{id}/update", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validUpdatePayload())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", equalTo(1404)));
    }

    @Test
    void updateReturns409WhenLoginNameTaken() throws Exception {
        authAsTeacher(1L);
        when(teacherStudentService.update(eq(3L), any(UpdateStudentRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.LOGIN_NAME_TAKEN, "登录名已被使用"));

        mockMvc.perform(post("/api/v1/teacher/students/{id}/update", 3L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validUpdatePayload())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", equalTo(2101)));
    }

    // ---------- 4.5 POST /teacher/students/{id}/delete ----------

    @Test
    void deleteReturns200OnSuccess() throws Exception {
        authAsTeacher(1L);

        mockMvc.perform(post("/api/v1/teacher/students/{id}/delete", 3L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", equalTo(0)));

        verify(teacherStudentService).delete(3L);
    }

    @Test
    void deleteReturns404WhenStudentMissing() throws Exception {
        authAsTeacher(1L);
        doThrow(new NotFoundException("学生不存在"))
                .when(teacherStudentService).delete(99L);

        mockMvc.perform(post("/api/v1/teacher/students/{id}/delete", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", equalTo(1404)));
    }
}
