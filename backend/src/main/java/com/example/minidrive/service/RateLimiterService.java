package com.example.minidrive.service;

import com.example.minidrive.enums.ErrorCode;
import com.example.minidrive.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimiterService {
    /*
     * Проверка cooldown и счётчика выполняется атомарно в Redis.
     * Результат: 0 — разрешено, положительное значение — cooldown,
     * отрицательное — оставшееся время текущего лимитного окна (мс).
     */
    private static final DefaultRedisScript<Long> CHECK_AND_RECORD_SCRIPT =
            new DefaultRedisScript<>("""
                local cooldown_ttl = redis.call('PTTL', KEYS[1])
                if cooldown_ttl > 0 then
                    return cooldown_ttl
                end

                local count = redis.call('INCR', KEYS[2])
                local window_ttl = redis.call('PTTL', KEYS[2])
                if count == 1 or window_ttl < 0 then
                    redis.call('PEXPIRE', KEYS[2], ARGV[2])
                    window_ttl = tonumber(ARGV[2])
                end

                if count > tonumber(ARGV[3]) then
                    return -window_ttl
                end

                redis.call('SET', KEYS[1], '1', 'PX', ARGV[1])
                return 0
                """, Long.class);

    private final StringRedisTemplate redisTemplate;

    /**
     * Проверяет и записывает попытку в Redis атомарно.
     *
     * @param actionKey       стабильное имя действия, например password_reset
     * @param identifier      нормализованный email, user ID или IP
     * @param cooldown        минимальный интервал между запросами
     * @param maxInWindow     максимальное число запросов в одном окне
     * @param window          длина лимитного окна
     */
    public void checkAndRecord(
            String actionKey,
            String identifier,
            Duration cooldown,
            int maxInWindow,
            Duration window
    ) {
        if (actionKey == null || actionKey.isBlank()
                || identifier == null || identifier.isBlank()
                || cooldown == null || cooldown.isNegative()
                || window == null || window.isZero() || window.isNegative()
                || maxInWindow < 1) {
            throw new IllegalArgumentException("Не верные параметры rate limiter");
        }

        String keyPrefix = "rate_limit:" + actionKey + ":" + identifier;
        long result = redisTemplate.execute(
                CHECK_AND_RECORD_SCRIPT,
                List.of(keyPrefix + ":cooldown", keyPrefix + ":window"),
                Long.toString(Math.max(1, cooldown.toMillis())),
                Long.toString(window.toMillis()),
                Integer.toString(maxInWindow)
        );

        if (result > 0) {
            long retryAfterSeconds = Math.max(1, (result + 999) / 1000);
            log.warn("Отклонён запрос действия {} из-за cooldown; повторить через {} сек.",
                    actionKey, retryAfterSeconds);
            throw new ApiException(
                    ErrorCode.TOO_MANY_REQUESTS,
                    Map.of("retryAfterSeconds", retryAfterSeconds)
            );
        }

        if (result < 0) {
            long retryAfterSeconds = Math.max(1, (-result + 999) / 1000);
            log.warn("Отклонён запрос действия {} из-за превышения окна лимита; повторить через {} сек.",
                    actionKey, retryAfterSeconds);
            throw new ApiException(
                    ErrorCode.RATE_LIMIT_EXCEEDED,
                    Map.of("retryAfterSeconds", retryAfterSeconds)
            );
        }
    }
}
