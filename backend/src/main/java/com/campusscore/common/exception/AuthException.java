package com.campusscore.common.exception;

import com.campusscore.common.ErrorCode;

/** 未认证（401 / 1401）。 */
public class AuthException extends BusinessException {
    private static final long serialVersionUID = 1L;

    public AuthException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }

    public AuthException(String message, Throwable cause) {
        super(ErrorCode.UNAUTHORIZED, message, cause);
    }
}
