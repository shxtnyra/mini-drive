package com.example.minidrive.dto.storage;

import com.example.minidrive.enums.StorageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public class StorageRequests {
    public record Create(
            @NotBlank String name,
            @NotNull StorageType type,
            @NotNull Map<String, Object> config  // JSON конфигурация
    ) {}
}
