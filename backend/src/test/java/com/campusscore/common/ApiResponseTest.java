package com.campusscore.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 断言 {@link ApiResponse} 的 JSON 表示与 {@code contracts/api.md §0.1}
 * 一致：成功不含 {@code data} 键的 null 输出、错误响应可携带 {@code errors[]}、
 * 字段错误结构符合 §0.3。
 */
class ApiResponseTest {

    private ObjectMapper json;

    @BeforeEach
    void setUp() {
        json = new ObjectMapper();
    }

    @Test
    void okDataSerializesWithCodeZeroAndOkMessage() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("hello", "world");
        String s = json.writeValueAsString(ApiResponse.ok(payload));
        assertThat(s)
                .contains("\"code\":0")
                .contains("\"message\":\"ok\"")
                .contains("\"hello\":\"world\"")
                .doesNotContain("\"errors\"");
    }

    @Test
    void okNoDataOmitsDataKey() throws Exception {
        String s = json.writeValueAsString(ApiResponse.ok());
        assertThat(s)
                .contains("\"code\":0")
                .doesNotContain("\"data\"")
                .doesNotContain("\"errors\"");
    }

    @Test
    void validationErrorCarriesErrorsArray() throws Exception {
        ApiResponse<Void> resp =
                ApiResponse.error(
                        ErrorCode.VALIDATION_FAILED,
                        "请求参数校验失败",
                        Arrays.asList(
                                FieldError.of("loginName", "VALIDATION_REQUIRED", "登录名必填"),
                                FieldError.of("tel", "VALIDATION_PATTERN", "格式不符")));
        String s = json.writeValueAsString(resp);
        assertThat(s)
                .contains("\"code\":1000")
                .contains("\"message\":\"请求参数校验失败\"")
                .contains("\"field\":\"loginName\"")
                .contains("\"code\":\"VALIDATION_REQUIRED\"")
                .contains("\"field\":\"tel\"");
    }

    @Test
    void businessErrorHasCodeAndNoErrorsWhenNull() throws Exception {
        String s =
                json.writeValueAsString(
                        ApiResponse.error(ErrorCode.LOGIN_NAME_TAKEN, "登录名已存在"));
        assertThat(s).contains("\"code\":2101").doesNotContain("\"errors\"");
    }

    @Test
    void errorCodeEnumHasStableNumbers() {
        assertThat(ErrorCode.SUCCESS.code()).isZero();
        assertThat(ErrorCode.VALIDATION_FAILED.code()).isEqualTo(1000);
        assertThat(ErrorCode.UNAUTHORIZED.code()).isEqualTo(1401);
        assertThat(ErrorCode.FORBIDDEN.code()).isEqualTo(1403);
        assertThat(ErrorCode.NOT_FOUND.code()).isEqualTo(1404);
        assertThat(ErrorCode.CONFLICT.code()).isEqualTo(1409);
        assertThat(ErrorCode.INTERNAL_ERROR.code()).isEqualTo(1500);
        assertThat(ErrorCode.LOGIN_FAILED.code()).isEqualTo(2001);
        assertThat(ErrorCode.LOGIN_NAME_TAKEN.code()).isEqualTo(2101);
        assertThat(ErrorCode.SUBJECT_DUPLICATE_FOR_TEACHER.code()).isEqualTo(2201);
        assertThat(ErrorCode.SUBJECT_HAS_SCORES.code()).isEqualTo(2202);
        assertThat(ErrorCode.SCORE_OUT_OF_RANGE.code()).isEqualTo(2301);
        assertThat(ErrorCode.SCORE_ALREADY_EXISTS.code()).isEqualTo(2302);
        assertThat(ErrorCode.SCORE_ALL_ENROLLED.code()).isEqualTo(2303);
        assertThat(ErrorCode.SCORE_SUBJECT_NOT_OWNED.code()).isEqualTo(2304);
    }

    @Test
    void errorCodeEnumMapsToHttpStatus() {
        assertThat(ErrorCode.UNAUTHORIZED.httpStatus()).isEqualTo(401);
        assertThat(ErrorCode.FORBIDDEN.httpStatus()).isEqualTo(403);
        assertThat(ErrorCode.NOT_FOUND.httpStatus()).isEqualTo(404);
        assertThat(ErrorCode.CONFLICT.httpStatus()).isEqualTo(409);
        assertThat(ErrorCode.INTERNAL_ERROR.httpStatus()).isEqualTo(500);
        assertThat(ErrorCode.SCORE_SUBJECT_NOT_OWNED.httpStatus()).isEqualTo(403);
        assertThat(ErrorCode.SCORE_ALREADY_EXISTS.httpStatus()).isEqualTo(409);
    }
}
