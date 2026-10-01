package com.example.minidrive.configuration.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.admin.bootstrap")
public record AdminBootstrapProperties(
        boolean enabled,
        String email,
        String username,
        String password
) {}
