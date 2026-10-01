package com.example.minidrive.model.redis;

public record PasswordResetData(
        String code,
        int failedAttempts
) implements VerificationCodeData {
    @Override
    public PasswordResetData withIncrementedAttempts() {
        return new PasswordResetData(code, failedAttempts + 1);
    }
}