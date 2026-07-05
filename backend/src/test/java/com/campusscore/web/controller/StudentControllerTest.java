package com.campusscore.web.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusscore.common.PageQuery;
import com.campusscore.common.PageResult;
import com.campusscore.domain.Role;
import com.campusscore.persistence.ScoreMapper;
import com.campusscore.persistence.TeacherMapper;
import com.campusscore.persistence.UserMapper;
import com.campusscore.security.AppUserDetails;
import com.campusscore.security.JwtService;
import com.campusscore.service.StudentScoreService;
import com.campusscore.web.advice.GlobalExceptionHandler;
import com.campusscore.web.dto.ScoreItemResponse;
import java.time.LocalDateTime;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** StudentController Web slice 测试 (T061)。 */
@WebMvcTest(controllers = StudentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = {
        "campusscore.jwt.secret=test-secret-please-change-me-please-change-me",
        "campusscore.jwt.access-ttl-minutes=30",
        "campusscore.jwt.refresh-ttl-days=7",
        "campusscore.jwt.issuer=test"
})
class StudentControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean StudentScoreService studentScoreService;
    @MockBean UserMapper userMapper;
    @MockBean TeacherMapper teacherMapper;
    @MockBean ScoreMapper scoreMapper;
    @MockBean com.campusscore.persistence.StudentMapper studentMapper;
    @MockBean com.campusscore.persistence.SubjectMapper subjectMapper;
    @MockBean JwtService jwtService;

    private void authAsStudent(long id) {
        AppUserDetails p = new AppUserDetails(id, "hhh", "x", Role.STUDENT);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(p, "x", p.getAuthorities()));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private ScoreItemResponse item(long id, int score) {
        return ScoreItemResponse.builder()
                .id(id)
                .subjectId(5L)
                .subjectName("Java EE")
                .teacherId(2L)
                .teacherName("伍老师")
                .grade(3)
                .score(score)
                .failing(score < 60)
                .updatedAt(LocalDateTime.of(2026, 7, 4, 12, 0))
                .build();
    }

    @Test
    void myScoresReturnsPagedItems() throws Exception {
        authAsStudent(3L);
        PageResult<ScoreItemResponse> page =
                PageResult.of(Arrays.asList(item(11L, 99), item(12L, 55)), 2L, 1, 50);
        when(studentScoreService.listMyScores(anyLong(), any(PageQuery.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/student/scores")
                        .param("page", "1")
                        .param("size", "50")
                        .param("sort", "grade,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", equalTo(0)))
                .andExpect(jsonPath("$.data.total", equalTo(2)))
                .andExpect(jsonPath("$.data.page", equalTo(1)))
                .andExpect(jsonPath("$.data.size", equalTo(50)))
                .andExpect(jsonPath("$.data.items[0].subjectName", equalTo("Java EE")))
                .andExpect(jsonPath("$.data.items[0].isFailing", equalTo(false)))
                .andExpect(jsonPath("$.data.items[1].isFailing", equalTo(true)));
    }

    @Test
    void myScoresClampsSizeAt100() throws Exception {
        authAsStudent(3L);
        when(studentScoreService.listMyScores(anyLong(), any(PageQuery.class)))
                .thenReturn(PageResult.empty(1, 100));

        mockMvc.perform(get("/api/v1/student/scores").param("size", "200"))
                .andExpect(status().isOk());

        ArgumentCaptor<PageQuery> cap = ArgumentCaptor.forClass(PageQuery.class);
        org.mockito.Mockito.verify(studentScoreService).listMyScores(org.mockito.ArgumentMatchers.eq(3L), cap.capture());
        org.assertj.core.api.Assertions.assertThat(cap.getValue().safeSize()).isEqualTo(100);
    }
}
