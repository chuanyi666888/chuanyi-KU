package com.smartgrain.common.exception;

import com.smartgrain.common.api.ResultCode;
import lombok.Getter;

import java.io.Serial;

/**
 * 业务异常 - 所有可预期的业务错误统一抛出此异常或其子类
 */
@Getter
public class BusinessException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ResultCode resultCode;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }

    public BusinessException(ResultCode resultCode, String message, Throwable cause) {
        super(message, cause);
        this.resultCode = resultCode;
    }

    public int getCode() {
        return resultCode.getCode();
    }
}
