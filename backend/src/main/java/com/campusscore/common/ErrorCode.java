package com.campusscore.common;

/**
 * 业务错误码枚举，对应 {@code contracts/api.md §0.3}。
 *
 * <p>HTTP 状态码由 {@link com.campusscore.web.advice.GlobalExceptionHandler} 统一映射；
 * 前端优先按 {@code code} 分派 UI。
 */
public enum ErrorCode {
    /** 成功。 */
    SUCCESS(0, 200),

    // ---------- 通用 ----------
    VALIDATION_FAILED(1000, 400),
    UNAUTHORIZED(1401, 401),
    FORBIDDEN(1403, 403),
    NOT_FOUND(1404, 404),
    CONFLICT(1409, 409),
    INTERNAL_ERROR(1500, 500),

    // ---------- Auth / Account ----------
    LOGIN_FAILED(2001, 400),
    OLD_PASSWORD_MISMATCH(2002, 400),
    CONFIRM_PASSWORD_MISMATCH(2003, 400),

    // ---------- Student CRUD ----------
    LOGIN_NAME_TAKEN(2101, 409),

    // ---------- Subject CRUD ----------
    SUBJECT_DUPLICATE_FOR_TEACHER(2201, 409),
    SUBJECT_HAS_SCORES(2202, 400),

    // ---------- Score CRUD ----------
    SCORE_OUT_OF_RANGE(2301, 400),
    SCORE_ALREADY_EXISTS(2302, 409),
    SCORE_ALL_ENROLLED(2303, 400),
    SCORE_SUBJECT_NOT_OWNED(2304, 403);

    private final int code;
    private final int httpStatus;

    ErrorCode(int code, int httpStatus) {
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public int code() {
        return code;
    }

    public int httpStatus() {
        return httpStatus;
    }
}
