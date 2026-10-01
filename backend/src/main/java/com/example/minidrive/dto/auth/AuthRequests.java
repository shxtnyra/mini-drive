package com.example.minidrive.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthRequests {
    public record Register(
            @NotBlank @Size(min = 3, max = 50) String username,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 128) String password,
            String displayName
    ) {}

    public record Login(
            @NotBlank String usernameOrEmail,
            @NotBlank String password
    ) {}

    public record Refresh(
            @NotBlank String refreshToken
    ) {}

    public record ChangePassword(
            @NotBlank String oldPassword,
            @NotBlank String newPassword
    ) {}

    public record PasswordReset(
            @NotBlank @Email String email
    ) {}

    public record VerifyPasswordReset(
            @NotBlank @Email String email,
            @NotBlank String code
    ) {}

    public record ConfirmPasswordReset(
            @NotBlank String token,
            @NotBlank String newPassword
    ) {}

    public record EmailChange(
            @Email String newEmail,
            @NotBlank String currentPassword
    ) {}

    public record VerifyEmail(
            @NotBlank String code
    ) {}
}
