package com.campusscore.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.campusscore.domain.Role;
import com.campusscore.security.AppUserDetails;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * 端点级越权矩阵（对应 tasks.md T170、宪章原则一 SC-006）。
 *
 * <p>断言：对 contracts §7 声明的所有受保护端点：
 * <ol>
 *   <li>未登录访问 → HTTP 401 + code=1401；
 *   <li>错误角色访问 → HTTP 403 + code=1403；
 *   <li>正确角色访问 → 不返回 401/403（可能 4xx 业务错，但绝不是 401/403）。
 * </ol>
 *
 * <p>使用 {@link TestFactory} 动态生成每个端点 3 条断言，便于日志定位失败端点。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityMatrixTest {

    @Autowired private MockMvc mockMvc;

    private enum Access {
        STUDENT,
        TEACHER,
        AUTHENTICATED
    }

    private static final class Endpoint {
        final String method;
        final String path;
        final Access access;
        final String bodyJson;

        Endpoint(String method, String path, Access access) {
            this(method, path, access, "{}");
        }

        Endpoint(String method, String path, Access access, String bodyJson) {
            this.method = method;
            this.path = path;
            this.access = access;
            this.bodyJson = bodyJson;
        }

        MockHttpServletRequestBuilder build() {
            switch (method) {
                case "GET":
                    return MockMvcRequestBuilders.get(path);
                case "POST":
                    return MockMvcRequestBuilders.post(path)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyJson);
                default:
                    throw new IllegalArgumentException(method);
            }
        }

        String label() {
            return method + " " + path + " (" + access + ")";
        }
    }

    /** 与 contracts §7 & SecurityConfig 保持同步。 */
    private static final List<Endpoint> ENDPOINTS = Arrays.asList(
            // ----- Auth-authenticated -----
            new Endpoint("POST", "/api/v1/auth/logout", Access.AUTHENTICATED),
            new Endpoint("GET", "/api/v1/auth/me", Access.AUTHENTICATED),
            // ----- Account (STUDENT or TEACHER) -----
            new Endpoint("GET", "/api/v1/account/profile", Access.AUTHENTICATED),
            new Endpoint(
                    "POST",
                    "/api/v1/account/profile/update",
                    Access.AUTHENTICATED,
                    "{\"tel\":\"18000000000\",\"address\":\"x\"}"),
            new Endpoint(
                    "POST",
                    "/api/v1/account/password/change",
                    Access.AUTHENTICATED,
                    "{\"oldPassword\":\"a\",\"newPassword\":\"aaaaaa\",\"confirmPassword\":\"aaaaaa\"}"),
            // ----- STUDENT -----
            new Endpoint("GET", "/api/v1/student/scores", Access.STUDENT),
            // ----- TEACHER: students -----
            new Endpoint("GET", "/api/v1/teacher/students", Access.TEACHER),
            new Endpoint("GET", "/api/v1/teacher/students/1", Access.TEACHER),
            new Endpoint(
                    "POST",
                    "/api/v1/teacher/students",
                    Access.TEACHER,
                    "{\"loginName\":\"x\",\"realName\":\"x\",\"password\":\"123456\",\"tel\":\"18000000000\",\"address\":\"x\",\"grade\":1}"),
            new Endpoint(
                    "POST",
                    "/api/v1/teacher/students/1/update",
                    Access.TEACHER,
                    "{\"loginName\":\"x\",\"realName\":\"x\",\"tel\":\"18000000000\",\"address\":\"x\",\"grade\":1}"),
            new Endpoint("POST", "/api/v1/teacher/students/1/delete", Access.TEACHER),
            // ----- TEACHER: subjects -----
            new Endpoint("GET", "/api/v1/teacher/subjects", Access.TEACHER),
            new Endpoint(
                    "POST",
                    "/api/v1/teacher/subjects",
                    Access.TEACHER,
                    "{\"name\":\"x\",\"grade\":1}"),
            new Endpoint(
                    "POST",
                    "/api/v1/teacher/subjects/1/update",
                    Access.TEACHER,
                    "{\"name\":\"x\",\"grade\":1}"),
            new Endpoint("POST", "/api/v1/teacher/subjects/1/delete", Access.TEACHER),
            // ----- TEACHER: scores -----
            new Endpoint("GET", "/api/v1/teacher/students/1/scores", Access.TEACHER),
            new Endpoint("GET", "/api/v1/teacher/students/1/entry-options", Access.TEACHER),
            new Endpoint(
                    "POST",
                    "/api/v1/teacher/students/1/scores",
                    Access.TEACHER,
                    "{\"subjectId\":1,\"score\":80}"),
            new Endpoint(
                    "POST",
                    "/api/v1/teacher/scores/1/update",
                    Access.TEACHER,
                    "{\"score\":80}"),
            new Endpoint("POST", "/api/v1/teacher/scores/1/delete", Access.TEACHER));

    private AppUserDetails student() {
        return new AppUserDetails(1L, "stu", null, Role.STUDENT);
    }

    private AppUserDetails teacher() {
        return new AppUserDetails(2L, "tea", null, Role.TEACHER);
    }

    @TestFactory
    @DisplayName("SC-006 越权矩阵：全部受保护端点 3 类断言")
    java.util.stream.Stream<DynamicTest> matrix() {
        return ENDPOINTS.stream()
                .flatMap(
                        ep -> java.util.stream.Stream.of(
                                DynamicTest.dynamicTest(
                                        "[UNAUTH 401/1401] " + ep.label(),
                                        () ->
                                                mockMvc.perform(ep.build())
                                                        .andExpect(
                                                                MockMvcResultMatchers.status()
                                                                        .isUnauthorized())
                                                        .andExpect(
                                                                MockMvcResultMatchers.jsonPath(
                                                                                "$.code")
                                                                        .value(1401))),
                                DynamicTest.dynamicTest(
                                        "[WRONG-ROLE 403/1403] " + ep.label(),
                                        () -> {
                                            AppUserDetails wrong;
                                            switch (ep.access) {
                                                case STUDENT:
                                                    wrong = teacher();
                                                    break;
                                                case TEACHER:
                                                    wrong = student();
                                                    break;
                                                default:
                                                    // AUTHENTICATED：无"错误角色"，跳过（学生/教师都合法）
                                                    return;
                                            }
                                            mockMvc.perform(ep.build().with(user(wrong)))
                                                    .andExpect(
                                                            MockMvcResultMatchers.status()
                                                                    .isForbidden())
                                                    .andExpect(
                                                            MockMvcResultMatchers.jsonPath(
                                                                            "$.code")
                                                                    .value(1403));
                                        }),
                                DynamicTest.dynamicTest(
                                        "[RIGHT-ROLE not 401/403] " + ep.label(),
                                        () -> {
                                            AppUserDetails right;
                                            switch (ep.access) {
                                                case STUDENT:
                                                    right = student();
                                                    break;
                                                case TEACHER:
                                                    right = teacher();
                                                    break;
                                                default:
                                                    right = student();
                                                    break;
                                            }
                                            int status =
                                                    mockMvc.perform(
                                                                    ep.build().with(user(right)))
                                                            .andReturn()
                                                            .getResponse()
                                                            .getStatus();
                                            org.assertj.core.api.Assertions.assertThat(status)
                                                    .as(
                                                            "端点 %s 正确角色不应返回 401/403，实际 %d",
                                                            ep.label(), status)
                                                    .isNotIn(401, 403);
                                        })));
    }
}
