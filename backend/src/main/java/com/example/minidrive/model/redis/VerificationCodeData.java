package com.example.minidrive.model.redis;

public interface VerificationCodeData {
    String code();
    int failedAttempts();
    VerificationCodeData withIncrementedAttempts();
}
