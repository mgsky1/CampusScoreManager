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

/** TeacherStudentController Web slice 测试 (T087)。 */
@WebMvcTest(controllers = TeacherStudentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = {
        "campusscore.jwt.secret=test-secret-please-change-me-please-change-me",
        "campusscore.jwt.access-ttl-minutes=30",
        "campusscore.jwt.refresh-ttl-days=7",
        "campusscore.jwt.issuer=test"
})
class TeacherStudentQueryControllerTest {

    @Autowired MockMvc mockMvc;

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

    private StudentListItem stu(long id, String realName, int grade) {
        return StudentListItem.builder()
                .id(id).loginName("s" + id).realName(realName)
                .tel("13800138000").address("SMU").grade(grade).build();
    }

    // ---------- 4.1 分页搜索 ----------

    @Test
    void searchReturnsPagedStudents() throws Exception {
        authAsTeacher(1L);
        PageResult<StudentListItem> page = PageResult.of(
                Arrays.asList(stu(3L, "黄同学", 3), stu(4L, "张三", 2)),
                2L, 1, 20);
        when(teacherStudentService.search(any(PageQuery.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/teacher/students")
                        .param("page", "1").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", equalTo(0)))
                .andExpect(jsonPath("$.data.total", equalTo(2)))
                .andExpect(jsonPath("$.data.page", equalTo(1)))
                .andExpect(jsonPath("$.data.size", equalTo(20)))
                .andExpect(jsonPath("$.data.hasNext", equalTo(false)))
                .andExpect(jsonPath("$.data.items[0].id", equalTo(3)))
                .andExpect(jsonPath("$.data.items[0].realName", equalTo("黄同学")))
                .andExpect(jsonPath("$.data.items[0].loginName", equalTo("s3")))
                .andExpect(jsonPath("$.data.items[1].grade", equalTo(2)));
    }

    @Test
    void searchPassesKeywordAndSortToService() throws Exception {
        authAsTeacher(1L);
        when(teacherStudentService.search(any(PageQuery.class))).thenReturn(PageResult.empty(1, 20));

        mockMvc.perform(get("/api/v1/teacher/students")
                        .param("keyword", "张")
                        .param("sort", "grade,desc"))
                .andExpect(status().isOk());

        ArgumentCaptor<PageQuery> cap = ArgumentCaptor.forClass(PageQuery.class);
        org.mockito.Mockito.verify(teacherStudentService).search(cap.capture());
        org.assertj.core.api.Assertions.assertThat(cap.getValue().getKeyword()).isEqualTo("张");
        org.assertj.core.api.Assertions.assertThat(cap.getValue().getSort()).isEqualTo("grade,desc");
    }

    @Test
    void searchClampsSizeAt100() throws Exception {
        authAsTeacher(1L);
        when(teacherStudentService.search(any(PageQuery.class))).thenReturn(PageResult.empty(1, 100));

        mockMvc.perform(get("/api/v1/teacher/students").param("size", "500"))
                .andExpect(status().isOk());

        ArgumentCaptor<PageQuery> cap = ArgumentCaptor.forClass(PageQuery.class);
        org.mockito.Mockito.verify(teacherStudentService).search(cap.capture());
        org.assertj.core.api.Assertions.assertThat(cap.getValue().safeSize()).isEqualTo(100);
    }

    // ---------- 4.2 详情 ----------

    @Test
    void detailReturnsStudent() throws Exception {
        authAsTeacher(1L);
        when(teacherStudentService.findById(3L)).thenReturn(stu(3L, "黄同学", 3));

        mockMvc.perform(get("/api/v1/teacher/students/{id}", 3L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", equalTo(0)))
                .andExpect(jsonPath("$.data.id", equalTo(3)))
                .andExpect(jsonPath("$.data.realName", equalTo("黄同学")))
                .andExpect(jsonPath("$.data.grade", equalTo(3)));
    }

    @Test
    void detailReturns404WhenMissing() throws Exception {
        authAsTeacher(1L);
        when(teacherStudentService.findById(anyLong())).thenThrow(new NotFoundException("学生不存在"));

        mockMvc.perform(get("/api/v1/teacher/students/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", equalTo(1404)));
    }
}
