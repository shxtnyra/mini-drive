package com.example.minidrive.service;

import com.example.minidrive.enums.ErrorCode;
import com.example.minidrive.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RateLimiterService {
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Проверяет кулдаун и количество запросов за окно времени.
     *
     * @param actionKey       Название действия (например, "send_verification_code")
     * @param identifier      Идентификатор пользователя (userId, email или IP)
     * @param cooldownSeconds Минимальный интервал между запросами (в секундах)
     * @param maxInWindow     Максимальное количество запросов за окно
     * @param windowMinutes   Размер окна (в минутах)
     */
    public void checkAndRecord(String actionKey, String identifier, int cooldownSeconds, int maxInWindow, int windowMinutes) {
        String cooldownKey = "rate_limit:cooldown:" + actionKey + ":" + identifier;
        String counterKey = "rate_limit:counter:" + actionKey + ":" + identifier;

        Boolean hasCooldown = redisTemplate.hasKey(cooldownKey);
        if (hasCooldown) {
            Long remainingTtl = redisTemplate.getExpire(cooldownKey, TimeUnit.SECONDS);
            long waitTime = (remainingTtl != null && remainingTtl > 0) ? remainingTtl : cooldownSeconds;

            if (waitTime > 0) {
                throw new ApiException(
                        ErrorCode.TOO_MANY_REQUESTS,
                        "Повторный запрос возможен через " + waitTime + " сек."
                );
            }
        }

        Long currentCount = redisTemplate.opsForValue().increment(counterKey);
        if (currentCount == 1) {
            redisTemplate.expire(counterKey, Duration.ofMinutes(windowMinutes));
        }

        if (currentCount > maxInWindow) {
            throw new ApiException(
                    ErrorCode.RATE_LIMIT_EXCEEDED,
                    "Превышен лимит запросов (" + maxInWindow + " в " + windowMinutes + " мин.). Попробуйте позже."
            );
        }

        redisTemplate.opsForValue().set(cooldownKey, "1", Duration.ofSeconds(cooldownSeconds));
    }
}
