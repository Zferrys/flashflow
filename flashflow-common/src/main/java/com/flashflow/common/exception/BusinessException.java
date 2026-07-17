package com.flashflow.common.exception;

import com.flashflow.common.domain.ErrorCode;
import lombok.Getter;

/**
 * 业务异常，由 GlobalExceptionHandler 统一拦截
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String extraMsg) {
        super(errorCode.getMessage() + "：" + extraMsg);
        this.code = errorCode.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
