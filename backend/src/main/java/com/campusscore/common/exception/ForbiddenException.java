package com.campusscore.common.exception;

import com.campusscore.common.ErrorCode;

/** 越权 / 归属不符（403 / 1403）。 */
public class ForbiddenException extends BusinessException {
    private static final long serialVersionUID = 1L;

    public ForbiddenException(String message) {
        super(ErrorCode.FORBIDDEN, message);
    }

    public ForbiddenException(ErrorCode ec, String message) {
        super(ec, message);
    }
}
