package com.campusscore.common.exception;

import com.campusscore.common.ErrorCode;

/** 资源不存在（404 / 1404）。 */
public class NotFoundException extends BusinessException {
    private static final long serialVersionUID = 1L;

    public NotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }
}
