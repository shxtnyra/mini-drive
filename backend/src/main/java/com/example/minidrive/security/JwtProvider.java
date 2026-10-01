package com.example.minidrive.security;

import com.example.minidrive.configuration.properties.JwtProperties;
import com.example.minidrive.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtProvider {
    private final SecretKey accessKey;
    private final JwtProperties jwtProperties;

    public JwtProvider(JwtProperties jwtProperties) {
        this.jwtProperties =jwtProperties;
        this.accessKey = Keys.hmacShaKeyFor(jwtProperties.accessSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user, Instant issuedAt) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("uuid", user.getUuid().toString())
                .claim("username", user.getUsername())
                .claim("is_admin", user.getIsAdmin())
                .claim("email", user.getEmail())
                .claim("email_verified", user.getEmailVerifiedAt() != null)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plusMillis(jwtProperties.accessExpirationMs())))
                .signWith(accessKey)
                .compact();
    }

    public String generatePasswordResetClaimToken(User user, Instant issuedAt) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("username", user.getUsername())
                .claim("email", user.getEmail())
                .claim("purpose", "password_change")
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plusMillis(600_000L)))
                .signWith(accessKey)
                .compact();
    }

    public String generateOpaqueRefreshToken() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public Claims getAccessClaims(String token) {
        return Jwts.parser()
                .verifyWith(accessKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
