package com.example.minidrive.service;

import com.example.minidrive.enums.ErrorCode;
import com.example.minidrive.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RateLimiterService {
    /*
     * Выполняет cooldown и фиксированное окно одним Lua-скриптом.
     * Redis запускает скрипт атомарно, поэтому два параллельных запроса
     * не могут одновременно пройти проверку cooldown и потерять инкремент.
     *
     * Результат: 0 означает разрешение, положительное число — остаток cooldown
     * в миллисекундах, отрицательное — остаток лимитного окна после превышения.
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
        Long result = redisTemplate.execute(
                CHECK_AND_RECORD_SCRIPT,
                List.of(keyPrefix + ":cooldown", keyPrefix + ":window"),
                Long.toString(Math.max(1, cooldown.toMillis())),
                Long.toString(window.toMillis()),
                Integer.toString(maxInWindow)
        );

        if (result == null) {
            throw new IllegalStateException("Redis не вернул результат");
        }

        if (result > 0) {
            long retryAfterSeconds = Math.max(1, (result + 999) / 1000);
            throw new ApiException(
                    ErrorCode.TOO_MANY_REQUESTS,
                    Map.of("retryAfterSeconds", retryAfterSeconds)
            );
        }

        if (result < 0) {
            long retryAfterSeconds = Math.max(1, (-result + 999) / 1000);
            throw new ApiException(
                    ErrorCode.RATE_LIMIT_EXCEEDED,
                    Map.of("retryAfterSeconds", retryAfterSeconds)
            );
        }
    }
}
