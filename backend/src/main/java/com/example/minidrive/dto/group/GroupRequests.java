package com.example.minidrive.dto.group;

import jakarta.validation.constraints.NotBlank;

public class GroupRequests {

    public record Create(
            @NotBlank String name
    ) {}

    public record Update(
            String name
    ) { }

    public record SetQuota(
            long quotaBytes
    ) {}
}
