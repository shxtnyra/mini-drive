package com.example.minidrive.configuration.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.email-code")
public record EmailCodeProperties(
        Duration expire,
        int maxAttempts,
        Duration cooldown,
        int maxRequestsPerHour
) {}
