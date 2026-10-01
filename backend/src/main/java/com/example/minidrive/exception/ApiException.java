package com.example.minidrive.exception;

import com.example.minidrive.enums.ErrorCode;
import lombok.Getter;

import java.util.Map;

@Getter
public class ApiException extends RuntimeException{
    private final ErrorCode errorCode;
    private final Map<String, Object> details;

    public ApiException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        details = null;
    }

    public ApiException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.details = details;
    }

    public ApiException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
        details = null;
    }
}
