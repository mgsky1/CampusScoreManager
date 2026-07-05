package com.campusscore.web.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
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
import com.campusscore.common.exception.NotFoundException;
import com.campusscore.domain.Role;
import com.campusscore.persistence.ScoreMapper;
import com.campusscore.persistence.StudentMapper;
import com.campusscore.persistence.SubjectMapper;
import com.campusscore.persistence.TeacherMapper;
import com.campusscore.persistence.UserMapper;
import com.campusscore.security.AppUserDetails;
import com.campusscore.security.JwtService;
import com.campusscore.service.TeacherScoreService;
import com.campusscore.web.advice.GlobalExceptionHandler;
import com.campusscore.web.dto.EntryOptionResponse;
import com.campusscore.web.dto.EntryOptionsResponse;
import com.campusscore.web.dto.StudentBriefResponse;
import com.campusscore.web.dto.TeacherScoreItemResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
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

/** TeacherScoreController Web slice 测试 (T089)。覆盖 contracts §6 全部端点 & 错误码。 */
@WebMvcTest(controllers = TeacherScoreController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = {
        "campusscore.jwt.secret=test-secret-please-change-me-please-change-me",
        "campusscore.jwt.access-ttl-minutes=30",
        "campusscore.jwt.refresh-ttl-days=7",
        "campusscore.jwt.issuer=test"
})
class TeacherScoreControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean TeacherScoreService teacherScoreService;
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

    private TeacherScoreItemResponse item(long id, long subjectId, String subjectName,
                                          int grade, int score, boolean editable) {
        return TeacherScoreItemResponse.builder()
                .id(id).subjectId(subjectId).subjectName(subjectName)
                .grade(grade).score(score).failing(score < 60).editable(editable)
                .updatedAt(LocalDateTime.of(2026, 7, 4, 12, 0))
                .build();
    }

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    // ---------- 6.1 GET /teacher/students/{sid}/scores ----------

    @Test
    void listStudentScoresReturnsPaged() throws Exception {
        authAsTeacher(1L);
        PageResult<TeacherScoreItemResponse> page = PageResult.of(Arrays.asList(
                item(11L, 5L, "Java EE", 3, 99, true),
                item(12L, 8L, "旧课程", 1, 55, false)), 2L, 1, 20);
        when(teacherScoreService.listScoresForStudent(anyLong(), anyLong(), any(PageQuery.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/teacher/students/{sid}/scores", 3L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", equalTo(0)))
                .andExpect(jsonPath("$.data.total", equalTo(2)))
                .andExpect(jsonPath("$.data.items[0].isFailing", equalTo(false)))
                .andExpect(jsonPath("$.data.items[0].editable", equalTo(true)))
                .andExpect(jsonPath("$.data.items[1].isFailing", equalTo(true)))
                .andExpect(jsonPath("$.data.items[1].editable", equalTo(false)));
    }

    @Test
    void listStudentScoresReturns404WhenStudentMissing() throws Exception {
        authAsTeacher(1L);
        when(teacherScoreService.listScoresForStudent(anyLong(), anyLong(), any(PageQuery.class)))
                .thenThrow(new NotFoundException("学生不存在"));

        mockMvc.perform(get("/api/v1/teacher/students/{sid}/scores", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", equalTo(1404)));
    }

    // ---------- 6.2 GET entry-options ----------

    @Test
    void entryOptionsReturnsOptionsAndStudent() throws Exception {
        authAsTeacher(1L);
        EntryOptionsResponse resp = EntryOptionsResponse.builder()
                .student(StudentBriefResponse.builder().id(3L).realName("黄同学").grade(3).build())
                .options(Arrays.asList(EntryOptionResponse.builder()
                        .subjectId(5L).subjectName("Java EE").grade(3).build()))
                .allEntered(false)
                .build();
        when(teacherScoreService.getEntryOptions(1L, 3L)).thenReturn(resp);

        mockMvc.perform(get("/api/v1/teacher/students/{sid}/entry-options", 3L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.student.realName", equalTo("黄同学")))
                .andExpect(jsonPath("$.data.options[0].subjectId", equalTo(5)))
                .andExpect(jsonPath("$.data.allEntered", equalTo(false)));
    }

    @Test
    void entryOptionsReturnsAllEnteredTrue() throws Exception {
        authAsTeacher(1L);
        EntryOptionsResponse resp = EntryOptionsResponse.builder()
                .student(StudentBriefResponse.builder().id(3L).realName("黄同学").grade(3).build())
                .options(Collections.emptyList())
                .allEntered(true)
                .build();
        when(teacherScoreService.getEntryOptions(1L, 3L)).thenReturn(resp);

        mockMvc.perform(get("/api/v1/teacher/students/{sid}/entry-options", 3L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allEntered", equalTo(true)))
                .andExpect(jsonPath("$.data.options.length()", equalTo(0)));
    }

    // ---------- 6.3 POST /teacher/students/{sid}/scores ----------

    @Test
    void createScoreReturns201() throws Exception {
        authAsTeacher(1L);
        when(teacherScoreService.createScore(anyLong(), anyLong(), anyLong(), anyInt()))
                .thenReturn(item(101L, 5L, "Java EE", 3, 88, true));
        Map<String, Object> body = new HashMap<>();
        body.put("subjectId", 5);
        body.put("score", 88);

        mockMvc.perform(post("/api/v1/teacher/students/{sid}/scores", 3L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code", equalTo(0)))
                .andExpect(jsonPath("$.data.id", equalTo(101)))
                .andExpect(jsonPath("$.data.editable", equalTo(true)));
    }

    @Test
    void createScoreValidationFailsWhenSubjectIdMissing() throws Exception {
        authAsTeacher(1L);
        Map<String, Object> body = new HashMap<>();
        body.put("score", 88);

        mockMvc.perform(post("/api/v1/teacher/students/{sid}/scores", 3L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo(1000)));
    }

    @Test
    void createScoreValidationFailsWhenScoreOutOfRange() throws Exception {
        authAsTeacher(1L);
        Map<String, Object> body = new HashMap<>();
        body.put("subjectId", 5);
        body.put("score", 101);

        mockMvc.perform(post("/api/v1/teacher/students/{sid}/scores", 3L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo(1000)));
    }

    @Test
    void createScoreReturns2304WhenSubjectNotOwned() throws Exception {
        authAsTeacher(1L);
        when(teacherScoreService.createScore(anyLong(), anyLong(), anyLong(), anyInt()))
                .thenThrow(new BusinessException(ErrorCode.SCORE_SUBJECT_NOT_OWNED, "not owned"));
        Map<String, Object> body = new HashMap<>();
        body.put("subjectId", 5);
        body.put("score", 88);

        mockMvc.perform(post("/api/v1/teacher/students/{sid}/scores", 3L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", equalTo(2304)));
    }

    @Test
    void createScoreReturns2302WhenDuplicate() throws Exception {
        authAsTeacher(1L);
        when(teacherScoreService.createScore(anyLong(), anyLong(), anyLong(), anyInt()))
                .thenThrow(new BusinessException(ErrorCode.SCORE_ALREADY_EXISTS, "duplicate"));
        Map<String, Object> body = new HashMap<>();
        body.put("subjectId", 5);
        body.put("score", 88);

        mockMvc.perform(post("/api/v1/teacher/students/{sid}/scores", 3L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", equalTo(2302)));
    }

    @Test
    void createScoreReturns2303WhenAllEntered() throws Exception {
        authAsTeacher(1L);
        when(teacherScoreService.createScore(anyLong(), anyLong(), anyLong(), anyInt()))
                .thenThrow(new BusinessException(ErrorCode.SCORE_ALL_ENROLLED, "all entered"));
        Map<String, Object> body = new HashMap<>();
        body.put("subjectId", 5);
        body.put("score", 88);

        mockMvc.perform(post("/api/v1/teacher/students/{sid}/scores", 3L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo(2303)));
    }

    @Test
    void createScoreReturns404WhenStudentMissing() throws Exception {
        authAsTeacher(1L);
        when(teacherScoreService.createScore(anyLong(), anyLong(), anyLong(), anyInt()))
                .thenThrow(new NotFoundException("学生不存在"));
        Map<String, Object> body = new HashMap<>();
        body.put("subjectId", 5);
        body.put("score", 88);

        mockMvc.perform(post("/api/v1/teacher/students/{sid}/scores", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", equalTo(1404)));
    }

    // ---------- 6.4 POST /teacher/scores/{scoreId}/update ----------

    @Test
    void updateScoreReturns200() throws Exception {
        authAsTeacher(1L);
        when(teacherScoreService.updateScore(1L, 101L, 85))
                .thenReturn(item(101L, 5L, "Java EE", 3, 85, true));
        Map<String, Object> body = new HashMap<>();
        body.put("score", 85);

        mockMvc.perform(post("/api/v1/teacher/scores/{scoreId}/update", 101L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.score", equalTo(85)))
                .andExpect(jsonPath("$.data.isFailing", equalTo(false)));
    }

    @Test
    void updateScoreValidationFails() throws Exception {
        authAsTeacher(1L);
        Map<String, Object> body = new HashMap<>();
        body.put("score", -1);

        mockMvc.perform(post("/api/v1/teacher/scores/{scoreId}/update", 101L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo(1000)));
    }

    @Test
    void updateScoreReturns2304WhenNotOwned() throws Exception {
        authAsTeacher(1L);
        when(teacherScoreService.updateScore(anyLong(), anyLong(), anyInt()))
                .thenThrow(new BusinessException(ErrorCode.SCORE_SUBJECT_NOT_OWNED, "not owned"));
        Map<String, Object> body = new HashMap<>();
        body.put("score", 85);

        mockMvc.perform(post("/api/v1/teacher/scores/{scoreId}/update", 101L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", equalTo(2304)));
    }

    @Test
    void updateScoreReturns404WhenMissing() throws Exception {
        authAsTeacher(1L);
        when(teacherScoreService.updateScore(anyLong(), anyLong(), anyInt()))
                .thenThrow(new NotFoundException("成绩不存在"));
        Map<String, Object> body = new HashMap<>();
        body.put("score", 85);

        mockMvc.perform(post("/api/v1/teacher/scores/{scoreId}/update", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", equalTo(1404)));
    }

    // ---------- 6.5 POST /teacher/scores/{scoreId}/delete ----------

    @Test
    void deleteScoreReturns200() throws Exception {
        authAsTeacher(1L);
        mockMvc.perform(post("/api/v1/teacher/scores/{scoreId}/delete", 101L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", equalTo(0)));
        verify(teacherScoreService).deleteScore(1L, 101L);
    }

    @Test
    void deleteScoreReturns2304WhenNotOwned() throws Exception {
        authAsTeacher(1L);
        doThrow(new BusinessException(ErrorCode.SCORE_SUBJECT_NOT_OWNED, "not owned"))
                .when(teacherScoreService).deleteScore(anyLong(), anyLong());

        mockMvc.perform(post("/api/v1/teacher/scores/{scoreId}/delete", 101L))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", equalTo(2304)));
    }

    @Test
    void deleteScoreReturns404WhenMissing() throws Exception {
        authAsTeacher(1L);
        doThrow(new NotFoundException("成绩不存在"))
                .when(teacherScoreService).deleteScore(anyLong(), anyLong());

        mockMvc.perform(post("/api/v1/teacher/scores/{scoreId}/delete", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", equalTo(1404)));
    }
}
