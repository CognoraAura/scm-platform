package com.scmcloud.common.exception;

import lombok.Getter;

/**
 * Business exception for the SCM Platform.
 * Supports both legacy integer codes and new ErrorCode enum.
 *
 * @author Deng
 * @since 2025-10-15
 */
@Getter
public class BusinessException extends RuntimeException {
    private final Integer code;
    private final ErrorCode errorCode;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
        this.errorCode = ErrorCode.BUSINESS_ERROR;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.errorCode = ErrorCode.BUSINESS_ERROR;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getI18nKey());
        this.code = errorCode.getCode();
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, Object... args) {
        super(String.format(errorCode.getI18nKey(), args));
        this.code = errorCode.getCode();
        this.errorCode = errorCode;
    }
}
