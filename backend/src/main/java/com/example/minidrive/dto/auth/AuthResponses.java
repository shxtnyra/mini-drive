package com.example.minidrive.dto.auth;

import com.example.minidrive.entity.RefreshToken;

import java.time.Instant;

public class AuthResponses {
    public record Session(
            String refreshToken,
            String ipAddress,
            String userAgent,
            String deviceName,
            Instant lastUsedAt
    ) {
        public static Session fromEntity(RefreshToken refreshToken) {
            return new Session(
                    refreshToken.getTokenHash(),
                    refreshToken.getIpAddress(),
                    refreshToken.getUserAgent(),
                    refreshToken.getDeviceName(),
                    refreshToken.getLastUsedAt()
            );
        }
    }

    public record Tokens(
            String accessToken,
            String refreshToken
    ) {}
}
