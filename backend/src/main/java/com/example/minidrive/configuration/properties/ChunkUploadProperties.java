package com.example.minidrive.configuration.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.chunk")
public record ChunkUploadProperties(
        long minSize,
        long maxSize,
        long defaultSize,
        Duration sessionExpire
) {}
