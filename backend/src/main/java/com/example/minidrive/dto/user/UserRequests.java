package com.example.minidrive.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserRequests {
    public record UpdateProfile(
            @NotBlank @Size(max = 255) String displayName
    ) {}

    public record UpdatePassword(
            String newPassword
    ) {}

    public record UpdateQuota(
            long newQuotaBytes
    ) {}
}
