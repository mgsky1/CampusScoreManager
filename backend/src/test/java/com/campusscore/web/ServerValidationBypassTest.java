package com.campusscore.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusscore.domain.Role;
import com.campusscore.security.AppUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * 服务端校验兜底（tasks.md T171、SC-007）。
 *
 * <p>模拟"前端绕过"场景：直接向所有写端点投递非法请求体，断言服务端返回：
 * <ul>
 *   <li>Bean Validation 违规 → HTTP 400 + code=1000；
 *   <li>业务规则违规 → HTTP 4xx + 2xxx 错误码。
 * </ul>
 *
 * <p>本测试假设 seed 数据未包含 login_name="__bypass_dup__"，故不会真的落库。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ServerValidationBypassTest {

    @Autowired private MockMvc mockMvc;

    private AppUserDetails student() {
        return new AppUserDetails(1L, "stu", null, Role.STUDENT);
    }

    private AppUserDetails teacher() {
        return new AppUserDetails(2L, "tea", null, Role.TEACHER);
    }

    // ---------- Account (需要认证的写端点) ----------

    @Test
    void updateProfileMissingTelReturns1000() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/v1/account/profile/update")
                                .with(user(teacher()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1000));
    }

    @Test
    void updateProfileMalformedTelReturns1000() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/v1/account/profile/update")
                                .with(user(teacher()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"tel\":\"abcxyz\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1000));
    }

    @Test
    void changePasswordConfirmMismatchReturns1000() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/v1/account/password/change")
                                .with(user(teacher()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"oldPassword\":\"x\",\"newPassword\":\"aaaaaa\",\"confirmPassword\":\"bbbbbb\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1000));
    }

    @Test
    void changePasswordTooShortReturns1000() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/v1/account/password/change")
                                .with(user(teacher()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"oldPassword\":\"x\",\"newPassword\":\"aa\",\"confirmPassword\":\"aa\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1000));
    }

    // ---------- Teacher: students ----------

    @Test
    void createStudentMissingAllReturns1000() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/v1/teacher/students")
                                .with(user(teacher()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1000));
    }

    @Test
    void createStudentInvalidGradeReturns1000() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/v1/teacher/students")
                                .with(user(teacher()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"loginName\":\"__bypass_x1__\",\"realName\":\"x\",\"password\":\"123456\",\"tel\":\"18000000000\",\"address\":\"x\",\"grade\":99}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1000));
    }

    @Test
    void updateStudentInvalidTelReturns1000() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/v1/teacher/students/1/update")
                                .with(user(teacher()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"loginName\":\"x\",\"realName\":\"x\",\"tel\":\"bad\",\"address\":\"x\",\"grade\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1000));
    }

    // ---------- Teacher: subjects ----------

    @Test
    void createSubjectMissingNameReturns1000() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/v1/teacher/subjects")
                                .with(user(teacher()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"grade\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1000));
    }

    @Test
    void createSubjectInvalidGradeReturns1000() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/v1/teacher/subjects")
                                .with(user(teacher()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"x\",\"grade\":99}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1000));
    }

    @Test
    void updateSubjectMissingNameReturns1000() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/v1/teacher/subjects/1/update")
                                .with(user(teacher()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"grade\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1000));
    }

    // ---------- Teacher: scores ----------

    @Test
    void createScoreMissingBodyReturns1000() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/v1/teacher/students/1/scores")
                                .with(user(teacher()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1000));
    }

    @Test
    void createScoreOutOfRangeReturns1000() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/v1/teacher/students/1/scores")
                                .with(user(teacher()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"subjectId\":1,\"score\":1000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1000));
    }

    @Test
    void updateScoreOutOfRangeReturns1000() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/v1/teacher/scores/1/update")
                                .with(user(teacher()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"score\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1000));
    }

    // ---------- Student 端点也做兜底 ----------

    @Test
    void studentScoresWithTeacherReturns403() throws Exception {
        // 前端应做 role 检查；即便前端绕过，服务端也应 403（这里补一条以覆盖 SC-007）
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/v1/student/scores").with(user(teacher())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1403));
    }
}
