package com.campusscore.common.exception;

import com.campusscore.common.ErrorCode;

/** 唯一性冲突 / 状态冲突（409 / 1409 或 2xxx 子类）。 */
public class ConflictException extends BusinessException {
    private static final long serialVersionUID = 1L;

    public ConflictException(String message) {
        super(ErrorCode.CONFLICT, message);
    }

    public ConflictException(ErrorCode ec, String message) {
        super(ec, message);
    }
}
