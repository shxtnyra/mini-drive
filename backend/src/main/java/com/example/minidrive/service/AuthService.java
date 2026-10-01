package com.example.minidrive.service;

import com.example.minidrive.configuration.properties.EmailCodeProperties;
import com.example.minidrive.dto.auth.*;
import com.example.minidrive.dto.common.ClientInfo;
import com.example.minidrive.entity.RefreshToken;
import com.example.minidrive.entity.User;
import com.example.minidrive.enums.ErrorCode;
import com.example.minidrive.exception.ApiException;
import com.example.minidrive.model.redis.EmailVerificationData;
import com.example.minidrive.model.redis.PasswordResetData;
import com.example.minidrive.model.redis.VerificationCodeData;
import com.example.minidrive.repository.RefreshTokenRepository;
import com.example.minidrive.repository.UserRepository;
import com.example.minidrive.repository.redis.VerificationCodeRepository;
import com.example.minidrive.security.JwtProvider;
import com.example.minidrive.security.JwtUserDetails;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final EmailSendService emailSendService;
    private final RateLimiterService rateLimiterService;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationCodeRepository verificationCodeRepository;

    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    private final EmailCodeProperties emailCodeProperties;

    private static final long DEFAULT_REFRESH_TOKEN_EXPIRE_MS = 2592000000L;

    private AuthResponses.Tokens issueTokenPair(User user, ClientInfo clientInfo, Instant now) {
        String accessToken = jwtProvider.generateAccessToken(user, now);
        String refreshTokenStr = jwtProvider.generateOpaqueRefreshToken();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(refreshTokenStr))
                .ipAddress(clientInfo != null ? clientInfo.ipAddress() : null)
                .userAgent(clientInfo != null ? clientInfo.userAgent() : null)
                .deviceName(clientInfo != null ? clientInfo.deviceName() : "Неизвестное устройство")
                .createdAt(now)
                .lastUsedAt(now)
                .expiresAt(now.plusMillis(DEFAULT_REFRESH_TOKEN_EXPIRE_MS))
                .build();

        refreshTokenRepository.save(refreshToken);

        return new AuthResponses.Tokens(
                accessToken,
                refreshTokenStr
        );
    }

    @Transactional
    public AuthResponses.Tokens register(AuthRequests.Register request, ClientInfo clientInfo) {
        if (userRepository.existsByEmail(request.email()))
            throw new ApiException(ErrorCode.EMAIL_EXISTS);

        if (userRepository.existsByUsername(request.username()))
            throw new ApiException(ErrorCode.USERNAME_EXISTS);

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .displayName(request.displayName())
                .build();

        User savedUser = userRepository.save(user);
        Instant now = clock.instant();
        String verificationCode = generateSixDigitCode();

        EmailVerificationData data = new EmailVerificationData(verificationCode, null, 0);
        verificationCodeRepository.save(getVerificationKey(savedUser.getId()),
                data,
                emailCodeProperties.expire()
        );
        emailSendService.sendVerificationCode(user.getEmail(), verificationCode);
        return issueTokenPair(savedUser, clientInfo, now);
    }
    @Transactional
    public AuthResponses.Tokens login(AuthRequests.Login request, ClientInfo clientInfo) {
        User user;
        if (request.usernameOrEmail().contains("@")) {
            user = userRepository.findByEmail(request.usernameOrEmail())
                    .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS));
        } else {
            user = userRepository.findByUsername(request.usernameOrEmail())
                    .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS));
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash()))
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);

        Instant now = clock.instant();
        return issueTokenPair(user, clientInfo, now);
    }

    @Transactional
    public void revokeRefreshToken(String refreshToken) {
        String refreshTokenHash = hashToken(refreshToken);
        Instant now = clock.instant();
        refreshTokenRepository.revokeByTokenHash(refreshTokenHash, now);
    }

    @Transactional
    public void revokeAllUserTokens(UUID userUuid) {
        User user = findUserByUuidOrThrow(userUuid);
        Instant now = clock.instant();
        refreshTokenRepository.revokeAllByUserId(user.getId(), now);
    }

    @Transactional
    public AuthResponses.Tokens refreshTokens(String refreshTokenStr, ClientInfo clientInfo) {
        String tokenHash = hashToken(refreshTokenStr);

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REFRESH_TOKEN));

        Instant now = clock.instant();

        if (now.isAfter(refreshToken.getExpiresAt()))
            throw new ApiException(ErrorCode.REFRESH_TOKEN_EXPIRED);

        // TODO: Чёт оно не работет
        if (refreshToken.getRevokedAt() != null) {
            refreshTokenRepository.revokeAllByUserId(refreshToken.getUser().getId(), now);
            throw new ApiException(ErrorCode.TOKEN_STOLEN);
        }

        String newAccessToken = jwtProvider.generateAccessToken(refreshToken.getUser(), now);
        String newRefreshTokenStr = jwtProvider.generateOpaqueRefreshToken();

        refreshToken.setTokenHash(hashToken(newRefreshTokenStr));
        refreshToken.setExpiresAt(now.plusMillis(DEFAULT_REFRESH_TOKEN_EXPIRE_MS));
        refreshToken.setIpAddress(clientInfo != null ? clientInfo.ipAddress() : null);
        refreshToken.setUserAgent(clientInfo != null ? clientInfo.userAgent() : null);
        refreshToken.setDeviceName(clientInfo != null ? clientInfo.deviceName() : null);
        refreshToken.setLastUsedAt(now);

        return new AuthResponses.Tokens(newAccessToken, newRefreshTokenStr);
    }
    @Transactional
    public AuthResponses.Tokens verifyEmail(AuthRequests.VerifyEmail request, JwtUserDetails jwtUser, ClientInfo clientInfo) {
        User user = findUserByUuidOrThrow(jwtUser.getUuid());

        if (user.getEmailVerifiedAt() != null)
            throw new ApiException(ErrorCode.EMAIL_VERIFIED);

        String redisKey = getVerificationKey(user.getId());
        validateAndConsumeCode(redisKey, request.code());

        Instant now = clock.instant();
        user.setEmailVerifiedAt(now);

        return issueTokenPair(user, clientInfo, now);
    }

    @Transactional
    public void resendEmailVerificationCode(JwtUserDetails jwtUser) {
        User user = findUserByUuidOrThrow(jwtUser.getUuid());

        if (user.getEmailVerifiedAt() != null)
            throw new ApiException(ErrorCode.EMAIL_VERIFIED);

        rateLimiterService.checkAndRecord(
                "email_verification",
                user.getId().toString(),
                emailCodeProperties.cooldown().toSecondsPart(),
                emailCodeProperties.maxRequestsPerHour(),
                60
        );

        String code = generateSixDigitCode();
        EmailVerificationData data = new EmailVerificationData(code, null, 0);

        verificationCodeRepository.save(
                getVerificationKey(user.getId()),
                data,
                emailCodeProperties.expire()
        );

        emailSendService.sendVerificationCode(user.getEmail(), code);
    }
    @Transactional
    public void requestEmailChange(AuthRequests.EmailChange request, JwtUserDetails jwtUser) {
        User user = findUserByUuidOrThrow(jwtUser.getUuid());

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash()))
            throw new ApiException(ErrorCode.INVALID_OLD_PASSWORD);

        if (userRepository.existsByEmail(request.newEmail()))
            throw new ApiException(ErrorCode.EMAIL_EXISTS);


        String code = generateSixDigitCode();
        EmailVerificationData data = new EmailVerificationData(code, request.newEmail(), 0);

        verificationCodeRepository.save(
                getVerificationKey(user.getId()),
                data,
                emailCodeProperties.expire()
        );


        emailSendService.sendVerificationCode(request.newEmail(), code);
    }

    @Transactional
    public AuthResponses.Tokens confirmEmailChange(AuthRequests.VerifyEmail request, JwtUserDetails jwtUser,
                                                   ClientInfo clientInfo) {
        User user = findUserByUuidOrThrow(jwtUser.getUuid());

        String redisKey = getVerificationKey(user.getId());
        EmailVerificationData data = validateAndConsumeCode(redisKey, request.code());

        Instant now = clock.instant();
        user.setEmail(data.newEmail());
        user.setEmailVerifiedAt(now);

        return issueTokenPair(user, clientInfo, now);
    }
    @Transactional
    public void changePassword(JwtUserDetails jwtUser, AuthRequests.ChangePassword request) {
        if (request.oldPassword().equals(request.newPassword()))
            throw new ApiException(ErrorCode.SAME_PASSWORD);

        User user = findUserByUuidOrThrow(jwtUser.getUuid());
        Instant now = clock.instant();

        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash()))
            throw new ApiException(ErrorCode.INVALID_OLD_PASSWORD);

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));

        refreshTokenRepository.revokeAllByUserId(user.getId(), now);
        emailSendService.sendPasswordChangedNotification(user.getEmail());
    }

    @Transactional
    public void requestPasswordReset(AuthRequests.PasswordReset request) {
        rateLimiterService.checkAndRecord(
                "password_reset",
                request.email().toLowerCase(),
                emailCodeProperties.cooldown().toSecondsPart(),
                emailCodeProperties.maxRequestsPerHour(),
                60
        );

        userRepository.findByEmail(request.email()).ifPresent(user -> {
            String code = generateSixDigitCode();
            PasswordResetData data = new PasswordResetData(code, 0);

            verificationCodeRepository.save(
                    getPasswordResetKey(user.getId()),
                    data,
                    emailCodeProperties.expire()
            );

            emailSendService.sendPasswordResetCode(request.email(), code);
        });
    }
    @Transactional
    public AuthResponses.Tokens verifyPasswordResetCode(
            AuthRequests.VerifyPasswordReset request) {

        // Чтобы не показывать есть ли профиль
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CODE));

        String redisKey = getPasswordResetKey(user.getId());
        validateAndConsumeCode(redisKey, request.code());

        Instant now = clock.instant();
        // Выдаём короткий одноразовый JWT-токен только для шага установки нового пароля
        String passwordChangeToken = jwtProvider.generatePasswordResetClaimToken(user, now);

        return new AuthResponses.Tokens(passwordChangeToken, null);
    }

    @Transactional
    public void confirmPasswordReset(AuthRequests.ConfirmPasswordReset request) {
        Claims claims = jwtProvider.getAccessClaims(request.token());

        if (!"password_change".equals(claims.get("purpose", String.class))) {
            throw new ApiException(ErrorCode.INVALID_TOKEN_PURPOSE);
        }

        String key = getPasswordResetTokenKey(request.token());
        Instant now = clock.instant();

        Instant expiration = claims.getExpiration().toInstant();
        Duration remainingTtl = Duration.between(now, expiration);

        if (remainingTtl.isNegative() || remainingTtl.isZero()) {
            throw new ApiException(ErrorCode.PASSWORD_RESET_TOKEN_EXPIRED);
        }

        Boolean isFirstUse = verificationCodeRepository.markTokenAsUsedIfAbsent(key, remainingTtl);

        if (Boolean.FALSE.equals(isFirstUse)) {
            throw new ApiException(ErrorCode.TOKEN_ALREADY_USED);
        }

        UUID userUuid = UUID.fromString(claims.getSubject());
        User user = findUserByUuidOrThrow(userUuid);

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        refreshTokenRepository.revokeAllByUserId(user.getId(), now);
    }

    @PreAuthorize("isAuthenticated()")
    public List<AuthResponses.Session> getUserSessions(UUID uuid) {
        User user = findUserByUuidOrThrow(uuid);
        Instant now = clock.instant();

        return refreshTokenRepository.findAllActiveByUserId(user.getId(), now)
                .stream().map(AuthResponses.Session::fromEntity).toList();
    }

    // --- Private Methods ---
    // Ключи формирую здесь, а не в репозитории выуждено из за универсального для всех метода validateAndConsumeCode
    private String generateSixDigitCode() {
        int code = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(code);
    }

    private String getVerificationKey(Long userId) {
        return "email_verification:" + userId;
    }

    private String getPasswordResetKey(Long userId) {
        return "password_reset:" + userId;
    }

    private String getPasswordResetTokenKey(String token) {
        return "password_reset_token:" + token;
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    private <T extends VerificationCodeData> T validateAndConsumeCode(String redisKey, String inputCode) {
        T data = verificationCodeRepository.<T>findByKey(redisKey)
                .orElseThrow(() -> new ApiException(ErrorCode.NO_CODE_FOUND));

        if (data.failedAttempts() >= emailCodeProperties.maxAttempts()) {
            verificationCodeRepository.delete(redisKey);
            throw new ApiException(ErrorCode.TOO_MANY_ATTEMPTS);
        }

        if (!data.code().equals(inputCode)) {
            verificationCodeRepository.updateAttempts(redisKey, data);
            throw new ApiException(ErrorCode.INVALID_CODE);
        }

        verificationCodeRepository.delete(redisKey);
        return data;
    }

    private User findUserByUuidOrThrow(UUID uuid) {
        return userRepository.findByUuid(uuid)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }
}
