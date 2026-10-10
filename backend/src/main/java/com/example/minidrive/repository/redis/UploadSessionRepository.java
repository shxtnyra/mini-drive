package com.example.minidrive.repository.redis;

import com.example.minidrive.model.redis.ChunkPartInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class UploadSessionRepository {
    private final RedisTemplate<String, Object> redisTemplate;

    private String getPartsKey(UUID sessionUuid) {
        return "upload_session:" + sessionUuid + ":parts";
    }

    /**
     * Хранит только временный индекс загруженных частей в Redis.
     * Метаданные сессии и резерв квоты находятся в PostgreSQL, чтобы их можно
     * было восстановить и очистить даже после истечения Redis TTL или рестарта.
     */
    public void addUploadedPart(UUID sessionUuid, int partNumber, String eTag, Duration ttl) {
        String partsKey = getPartsKey(sessionUuid);
        redisTemplate.opsForHash().put(partsKey, String.valueOf(partNumber), eTag);
        redisTemplate.expire(partsKey, ttl);
    }

    public List<ChunkPartInfo> getUploadedParts(UUID sessionUuid) {
        String partsKey = getPartsKey(sessionUuid);

        Map<Object, Object> rawMap = redisTemplate.opsForHash().entries(partsKey);

        if (rawMap == null || rawMap.isEmpty()) {
            return List.of();
        }

        return rawMap.entrySet().stream()
                .map(entry -> new ChunkPartInfo(
                        Integer.parseInt(entry.getKey().toString()),
                        entry.getValue().toString()
                ))
                .sorted(Comparator.comparingInt(ChunkPartInfo::partNumber))
                .toList();
    }

    public void delete(UUID sessionUuid) {
        redisTemplate.delete(getPartsKey(sessionUuid));
    }
}
