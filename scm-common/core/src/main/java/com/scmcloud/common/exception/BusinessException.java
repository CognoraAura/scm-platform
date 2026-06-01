package com.scmcloud.common.exception;

import lombok.Getter;

/**
 * 业务异常�?
 *
 * @author Deng
 * createData 2025/10/15 14:26
 * @version 1.0
 */
@Getter
public class BusinessException extends RuntimeException {
    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
