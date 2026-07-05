package com.campusscore.common.exception;

import com.campusscore.common.ErrorCode;
import lombok.Getter;

/**
 * 业务异常基类。所有细分异常必须携带一个 {@link ErrorCode}。
 * {@link com.campusscore.web.advice.GlobalExceptionHandler} 会据此映射
 * HTTP 状态码与响应体 {@code code}。
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
