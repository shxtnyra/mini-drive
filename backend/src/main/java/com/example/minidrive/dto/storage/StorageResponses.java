package com.example.minidrive.dto.storage;

import com.example.minidrive.entity.Storage;
import com.example.minidrive.enums.StorageType;

import java.time.Instant;
import java.util.Map;

public class StorageResponses {
    public record Base(
            Long id,
            String name,
            StorageType type,
            Map<String, Object> config,
            boolean isDefault,
            Instant createAt
    ) {
        public static Base fromEntity(Storage storage) {
            return new Base(
                    storage.getId(),
                    storage.getName(),
                    storage.getType(),
                    storage.getConfig(),
                    storage.isDefault(),
                    storage.getCreatedAt()
            );
        }
    }
}
