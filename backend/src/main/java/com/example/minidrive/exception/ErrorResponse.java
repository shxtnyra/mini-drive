package com.example.minidrive.exception;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String message,
        String error,
        String path,
        Map<String, Object> details
) {
    public static ErrorResponse of(HttpStatus status, String errorCode, String message, String path) {
        return new ErrorResponse(Instant.now(), status.value(), message, errorCode, path, null);
    }

    public static ErrorResponse of(HttpStatus status, String errorCode, String message, String path, Map<String, Object> details) {
        return new ErrorResponse(Instant.now(), status.value(), message, errorCode, path, details);
    }
}
