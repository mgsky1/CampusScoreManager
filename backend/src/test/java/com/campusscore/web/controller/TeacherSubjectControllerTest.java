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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusscore.common.ErrorCode;
import com.campusscore.common.PageQuery;
import com.campusscore.common.PageResult;
import com.campusscore.common.exception.BusinessException;
import com.campusscore.common.exception.ForbiddenException;
import com.campusscore.common.exception.NotFoundException;
import com.campusscore.domain.Role;
import com.campusscore.domain.Subject;
import com.campusscore.persistence.ScoreMapper;
import com.campusscore.persistence.StudentMapper;
import com.campusscore.persistence.SubjectMapper;
import com.campusscore.persistence.TeacherMapper;
import com.campusscore.persistence.UserMapper;
import com.campusscore.security.AppUserDetails;
import com.campusscore.security.JwtService;
import com.campusscore.service.TeacherSubjectService;
import com.campusscore.web.advice.GlobalExceptionHandler;
import com.campusscore.web.dto.CreateSubjectRequest;
import com.campusscore.web.dto.UpdateSubjectRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
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

/** TeacherSubjectController Web slice 测试 (T134)。覆盖 contracts §5.1-5.4。 */
@WebMvcTest(controllers = TeacherSubjectController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = {
        "campusscore.jwt.secret=test-secret-please-change-me-please-change-me",
        "campusscore.jwt.access-ttl-minutes=30",
        "campusscore.jwt.refresh-ttl-days=7",
        "campusscore.jwt.issuer=test"
})
class TeacherSubjectControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean TeacherSubjectService teacherSubjectService;
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

    private Subject sub(long id, long teacherId, String name, int grade) {
        return Subject.builder().id(id).teacherId(teacherId).name(name).grade(grade).build();
    }

    private Map<String, Object> createBody(String name, int grade) {
        Map<String, Object> m = new HashMap<>();
        m.put("name", name);
        m.put("grade", grade);
        return m;
    }

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    // ---------- 5.1 GET ----------

    @Test
    void listMineReturnsPagedSubjects() throws Exception {
        authAsTeacher(1L);
        PageResult<Subject> page = PageResult.of(
                Arrays.asList(sub(1, 1, "计算机导论", 1), sub(2, 1, "汇编语言", 2)),
                2L, 1, 20);
        when(teacherSubjectService.listMine(eq(1L), any(PageQuery.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/teacher/subjects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", equalTo(0)))
                .andExpect(jsonPath("$.data.total", equalTo(2)))
                .andExpect(jsonPath("$.data.items[0].id", equalTo(1)))
                .andExpect(jsonPath("$.data.items[0].name", equalTo("计算机导论")))
                .andExpect(jsonPath("$.data.items[1].grade", equalTo(2)));
    }

    // ---------- 5.2 POST create ----------

    @Test
    void createReturns201WithSubject() throws Exception {
        authAsTeacher(1L);
        when(teacherSubjectService.create(eq(1L), any(CreateSubjectRequest.class)))
                .thenReturn(sub(999, 1, "云计算", 3));

        mockMvc.perform(post("/api/v1/teacher/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(createBody("云计算", 3))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code", equalTo(0)))
                .andExpect(jsonPath("$.data.id", equalTo(999)))
                .andExpect(jsonPath("$.data.name", equalTo("云计算")))
                .andExpect(jsonPath("$.data.grade", equalTo(3)));
    }

    @Test
    void createReturns400WhenNameBlank() throws Exception {
        authAsTeacher(1L);
        Map<String, Object> body = createBody("", 3);

        mockMvc.perform(post("/api/v1/teacher/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo(1000)))
                .andExpect(jsonPath("$.errors", notNullValue()));

        verify(teacherSubjectService, never()).create(anyLong(), any());
    }

    @Test
    void createReturns400WhenGradeOutOfRange() throws Exception {
        authAsTeacher(1L);
        mockMvc.perform(post("/api/v1/teacher/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(createBody("云计算", 9))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo(1000)));
    }

    @Test
    void createReturns409OnDuplicateName() throws Exception {
        authAsTeacher(1L);
        when(teacherSubjectService.create(eq(1L), any(CreateSubjectRequest.class)))
                .thenThrow(new BusinessException(
                        ErrorCode.SUBJECT_DUPLICATE_FOR_TEACHER, "该教师已存在同名课程"));

        mockMvc.perform(post("/api/v1/teacher/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(createBody("Java EE", 3))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", equalTo(2201)));
    }

    // ---------- 5.3 POST update ----------

    @Test
    void updateReturns200() throws Exception {
        authAsTeacher(1L);
        when(teacherSubjectService.update(eq(1L), eq(2L), any(UpdateSubjectRequest.class)))
                .thenReturn(sub(2, 1, "汇编语言进阶", 3));

        mockMvc.perform(post("/api/v1/teacher/subjects/{id}/update", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(createBody("汇编语言进阶", 3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", equalTo(0)))
                .andExpect(jsonPath("$.data.name", equalTo("汇编语言进阶")))
                .andExpect(jsonPath("$.data.grade", equalTo(3)));
    }

    @Test
    void updateReturns404WhenMissing() throws Exception {
        authAsTeacher(1L);
        when(teacherSubjectService.update(eq(1L), eq(999L), any(UpdateSubjectRequest.class)))
                .thenThrow(new NotFoundException("课程不存在"));

        mockMvc.perform(post("/api/v1/teacher/subjects/{id}/update", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(createBody("x", 1))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", equalTo(1404)));
    }

    @Test
    void updateReturns403WhenNotOwner() throws Exception {
        authAsTeacher(1L);
        when(teacherSubjectService.update(eq(1L), eq(5L), any(UpdateSubjectRequest.class)))
                .thenThrow(new ForbiddenException("该课程不属于当前教师"));

        mockMvc.perform(post("/api/v1/teacher/subjects/{id}/update", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(createBody("Java EE 改", 3))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", equalTo(1403)));
    }

    @Test
    void updateReturns409OnDuplicateName() throws Exception {
        authAsTeacher(1L);
        when(teacherSubjectService.update(eq(1L), eq(2L), any(UpdateSubjectRequest.class)))
                .thenThrow(new BusinessException(
                        ErrorCode.SUBJECT_DUPLICATE_FOR_TEACHER, "该教师已存在同名课程"));

        mockMvc.perform(post("/api/v1/teacher/subjects/{id}/update", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(createBody("计算机导论", 1))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", equalTo(2201)));
    }

    // ---------- 5.4 POST delete ----------

    @Test
    void deleteReturns200() throws Exception {
        authAsTeacher(1L);

        mockMvc.perform(post("/api/v1/teacher/subjects/{id}/delete", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", equalTo(0)));

        verify(teacherSubjectService).delete(1L, 2L);
    }

    @Test
    void deleteReturns404WhenMissing() throws Exception {
        authAsTeacher(1L);
        doThrow(new NotFoundException("课程不存在"))
                .when(teacherSubjectService).delete(1L, 999L);

        mockMvc.perform(post("/api/v1/teacher/subjects/{id}/delete", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", equalTo(1404)));
    }

    @Test
    void deleteReturns403WhenNotOwner() throws Exception {
        authAsTeacher(1L);
        doThrow(new ForbiddenException("该课程不属于当前教师"))
                .when(teacherSubjectService).delete(1L, 5L);

        mockMvc.perform(post("/api/v1/teacher/subjects/{id}/delete", 5L))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", equalTo(1403)));
    }

    @Test
    void deleteReturns400_2202WhenScoresExist() throws Exception {
        authAsTeacher(1L);
        doThrow(new BusinessException(
                ErrorCode.SUBJECT_HAS_SCORES,
                "该课程存在成绩记录，请先删除相关成绩或联系管理员归档"))
                .when(teacherSubjectService).delete(1L, 5L);

        mockMvc.perform(post("/api/v1/teacher/subjects/{id}/delete", 5L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo(2202)));
    }
}
