package com.campusscore.web.advice;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusscore.common.ErrorCode;
import com.campusscore.common.exception.AuthException;
import com.campusscore.common.exception.BusinessException;
import com.campusscore.common.exception.ConflictException;
import com.campusscore.common.exception.ForbiddenException;
import com.campusscore.common.exception.NotFoundException;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Boots a slice with a throw-away controller that raises each exception, then
 * asserts the JSON shape produced by {@link GlobalExceptionHandler}.
 */
@WebMvcTest(controllers = GlobalExceptionHandlerTest.ThrowingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.ThrowingController.class})
@EnableWebMvc
class GlobalExceptionHandlerTest {

    @Autowired private MockMvc mockMvc;

    // JwtAuthenticationFilter 被 componentscan 拉进来，需要 JwtService bean —
    // 但本测试专注异常映射，直接 mock 掉。UserMapper 同理。
    @org.springframework.boot.test.mock.mockito.MockBean
    private com.campusscore.security.JwtService jwtService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.campusscore.persistence.UserMapper userMapper;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.campusscore.persistence.ScoreMapper scoreMapper;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.campusscore.persistence.StudentMapper studentMapper;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.campusscore.persistence.TeacherMapper teacherMapper;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.campusscore.persistence.SubjectMapper subjectMapper;

    @Test
    void validationErrorReturnsCode1000WithFieldErrors() throws Exception {
        mockMvc.perform(
                        post("/__test/validate", "{\"loginName\":\"\",\"tel\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[?(@.field=='loginName')].code")
                        .value(org.hamcrest.Matchers.hasItem(containsString("VALIDATION"))))
                .andExpect(jsonPath("$.errors[?(@.field=='tel')].code")
                        .value(org.hamcrest.Matchers.hasItem(containsString("VALIDATION"))));
    }

    @Test
    void authExceptionReturns401AndCode1401() throws Exception {
        mockMvc.perform(post("/__test/auth", "{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1401));
    }

    @Test
    void forbiddenExceptionReturns403AndCode1403() throws Exception {
        mockMvc.perform(post("/__test/forbidden", "{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1403));
    }

    @Test
    void notFoundExceptionReturns404AndCode1404() throws Exception {
        mockMvc.perform(post("/__test/notfound", "{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1404));
    }

    @Test
    void conflictExceptionUsesEnumCode() throws Exception {
        mockMvc.perform(post("/__test/conflict", "{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(2101));
    }

    @Test
    void businessExceptionRespectsHttpStatusOfErrorCode() throws Exception {
        mockMvc.perform(post("/__test/business", "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(2301));
    }

    @Test
    void unknownRuntimeExceptionReturns500AndDoesNotLeakStack() throws Exception {
        mockMvc.perform(post("/__test/boom", "{}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(1500))
                .andExpect(jsonPath("$.message").exists())
                // stack trace must never appear in the response body
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(containsString("at "))));
    }

    // ---------- helpers ----------

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder post(
            String path, String body) {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    /** Throw-away controller exercising each exception path. */
    @RestController
    @RequestMapping("/__test")
    static class ThrowingController {

        @PostMapping("/validate")
        public String validate(@Valid @RequestBody ValidationPayload body) {
            return "ok";
        }

        @PostMapping("/auth")
        public String auth() {
            throw new AuthException("no token");
        }

        @PostMapping("/forbidden")
        public String forbidden() {
            throw new ForbiddenException("nope");
        }

        @PostMapping("/notfound")
        public String notfound() {
            throw new NotFoundException("gone");
        }

        @PostMapping("/conflict")
        public String conflict() {
            throw new ConflictException(ErrorCode.LOGIN_NAME_TAKEN, "登录名已存在");
        }

        @PostMapping("/business")
        public String business() {
            throw new BusinessException(ErrorCode.SCORE_OUT_OF_RANGE, "0-100");
        }

        @PostMapping("/boom")
        public String boom() {
            throw new IllegalStateException("something went wrong deep down");
        }
    }

    static class ValidationPayload {
        @NotBlank
        @Size(min = 1, max = 50)
        public String loginName;

        @javax.validation.constraints.Pattern(regexp = "\\d{8,11}")
        public String tel;
    }
}
