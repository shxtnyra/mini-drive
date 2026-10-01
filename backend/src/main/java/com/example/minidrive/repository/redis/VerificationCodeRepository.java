package com.example.minidrive.repository.redis;

import com.example.minidrive.model.redis.VerificationCodeData;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class VerificationCodeRepository {
    private final RedisTemplate<String, Object> redisTemplate;

    public  <T extends VerificationCodeData> void save(String key, T data, Duration ttl) {
        redisTemplate.opsForValue().set(key, data, ttl);
    }

    public  <T extends VerificationCodeData> Optional<T> findByKey(String key) {
        T data = (T) redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(data);
    }

    public <T extends VerificationCodeData> void updateAttempts(String key, T data) {
        Long remainingTtl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(key, data.withIncrementedAttempts(), Duration.ofSeconds(remainingTtl));
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public Boolean markTokenAsUsedIfAbsent(String key, Duration ttl) {
        return redisTemplate.opsForValue().setIfAbsent(key, ttl);
    }
}
