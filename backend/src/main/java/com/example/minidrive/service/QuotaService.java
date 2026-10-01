package com.example.minidrive.service;

import com.example.minidrive.repository.GroupRepository;
import com.example.minidrive.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuotaService {
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${app.quota.default-bytes:5368709120}")
    private long defaultQuotaBytes;

    private static final Duration QUOTA_CACHE_TTL = Duration.ofHours(24);

    private String getQuotaKey(Long userId) {
        return "user_quota:" + userId;
    }

    public long getUserQuota(Long userId) {
        String cacheKey = getQuotaKey(userId);
        Object cachedQuota = redisTemplate.opsForValue().get(cacheKey);

        if (cachedQuota != null) {
            return Long.parseLong(cachedQuota.toString());
        }

        long calculatedQuota = calculateUserQuota(userId);
        redisTemplate.opsForValue().set(cacheKey, String.valueOf(calculatedQuota), QUOTA_CACHE_TTL);

        return calculatedQuota;
    }

    /**
     * Расчет по приоритету:
     * 1. Индивидуальная квота пользователя (если заполнена в БД)
     * 2. Максимальная квота из всех групп, в которых состоит пользователь
     * 3. Дефолтная системная квота
     */
    private long calculateUserQuota(Long userId) {
        var user = userRepository.findById(userId).orElseThrow();
        if (user.getQuotaBytes() != null) {
            return user.getQuotaBytes();
        }

        return groupRepository.findMaxQuotaByUserId(userId)
                .orElse(defaultQuotaBytes);
    }

    public void invalidateUserQuota(Long userId) {
        redisTemplate.delete(getQuotaKey(userId));
    }

    public void invalidateGroupMembersQuota(Long groupId) {
        List<Long> memberIds = groupRepository.findMemberIdsByGroupId(groupId);
        List<String> keys = memberIds.stream().map(this::getQuotaKey).toList();
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}