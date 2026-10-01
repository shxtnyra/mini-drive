package com.example.minidrive.dto.common;

public record ClientInfo(
        String ipAddress,
        String userAgent,
        String deviceName
) {}
