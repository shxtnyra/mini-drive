package com.example.minidrive.configuration.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        long accessExpirationMs,
        long refreshExpirationMs,
        String accessSecret
) {}
