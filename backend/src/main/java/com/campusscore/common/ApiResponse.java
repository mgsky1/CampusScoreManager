package com.campusscore.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 统一响应信封，对应 {@code contracts/api.md §0.1}。
 *
 * <p>规范：{@code code=0} 为成功；非 0 为错误。{@code errors} 字段仅在
 * 字段校验类错误（{@link ErrorCode#VALIDATION_FAILED}）时携带。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private int code;
    private String message;
    private T data;
    private List<FieldError> errors;

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(ErrorCode.SUCCESS.code(), "ok", data, null);
    }

    public static <T> ApiResponse<T> ok() {
        return ok(null);
    }

    public static <T> ApiResponse<T> error(ErrorCode ec, String message) {
        return new ApiResponse<>(ec.code(), message, null, null);
    }

    public static <T> ApiResponse<T> error(ErrorCode ec, String message, List<FieldError> errors) {
        return new ApiResponse<>(
                ec.code(),
                message,
                null,
                errors == null || errors.isEmpty() ? null : Collections.unmodifiableList(errors));
    }
}
