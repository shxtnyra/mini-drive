package com.example.minidrive.model.redis;


public record EmailVerificationData(
        String code,
        String newEmail,
        int failedAttempts
) implements VerificationCodeData {
    @Override
    public EmailVerificationData withIncrementedAttempts() {
        return new EmailVerificationData(code, newEmail, failedAttempts + 1);
    }
}
