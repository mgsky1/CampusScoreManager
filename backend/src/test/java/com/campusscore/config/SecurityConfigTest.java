package com.campusscore.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusscore.domain.Role;
import com.campusscore.security.AppUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * 加载完整 Spring Security 链，断言 §0.6 的路径 × 角色矩阵。
 *
 * <p>这里不真实产生 JWT——用 Spring Security Test 的 {@code user()} 后处理器直接注入
 * 已鉴权主体。JWT 解析路径由 {@link com.campusscore.security.JwtServiceTest} 覆盖。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired private MockMvc mockMvc;

    private AppUserDetails student() {
        return new AppUserDetails(1L, "stu", null, Role.STUDENT);
    }

    private AppUserDetails teacher() {
        return new AppUserDetails(2L, "tea", null, Role.TEACHER);
    }

    // ---------- 公开端点 ----------

    @Test
    void loginIsPublic() throws Exception {
        int st =
                mockMvc.perform(
                                MockMvcRequestBuilders.post("/api/v1/auth/login")
                                        .contentType("application/json")
                                        .content("{}"))
                        .andReturn()
                        .getResponse()
                        .getStatus();
        // controller 尚未实现，Spring 返回 404；关键是不能是 401 / 403
        org.assertj.core.api.Assertions.assertThat(st).isNotIn(401, 403);
    }

    @Test
    void refreshIsPublic() throws Exception {
        int st =
                mockMvc.perform(
                                MockMvcRequestBuilders.post("/api/v1/auth/refresh")
                                        .contentType("application/json")
                                        .content("{}"))
                        .andReturn()
                        .getResponse()
                        .getStatus();
        org.assertj.core.api.Assertions.assertThat(st).isNotIn(401, 403);
    }

    // ---------- 未认证访问受保护端点 → 401 / 1401 ----------

    @Test
    void unauthenticatedStudentEndpointReturns401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/student/scores"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1401));
    }

    @Test
    void unauthenticatedTeacherEndpointReturns401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/teacher/subjects"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1401));
    }

    @Test
    void unauthenticatedAccountEndpointReturns401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/account/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1401));
    }

    // ---------- 越权：STUDENT 访问 /teacher/** → 403 / 1403 ----------

    @Test
    void studentAccessingTeacherEndpointReturns403() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/v1/teacher/subjects").with(user(student())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1403));
    }

    // ---------- 越权：TEACHER 访问 /student/** → 403 / 1403 ----------

    @Test
    void teacherAccessingStudentEndpointReturns403() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/v1/student/scores").with(user(teacher())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1403));
    }

    // ---------- 同角色访问自身端点 → 不 401 也不 403（可能 404，因为 controller 还没写） ----------

    @Test
    void studentAccessingStudentEndpointIsNotDenied() throws Exception {
        int status =
                mockMvc.perform(
                                MockMvcRequestBuilders.get("/api/v1/student/scores")
                                        .with(user(student())))
                        .andReturn()
                        .getResponse()
                        .getStatus();
        // controller 未实现，Spring 返回 404；关键是 status ≠ 401 && ≠ 403
        org.assertj.core.api.Assertions.assertThat(status).isNotIn(401, 403);
    }

    @Test
    void teacherAccessingTeacherEndpointIsNotDenied() throws Exception {
        int status =
                mockMvc.perform(
                                MockMvcRequestBuilders.get("/api/v1/teacher/subjects")
                                        .with(user(teacher())))
                        .andReturn()
                        .getResponse()
                        .getStatus();
        org.assertj.core.api.Assertions.assertThat(status).isNotIn(401, 403);
    }
}
