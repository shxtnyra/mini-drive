package com.example.minidrive.provider;

import com.example.minidrive.entity.Storage;
import com.example.minidrive.enums.ErrorCode;
import com.example.minidrive.exception.ApiException;
import com.example.minidrive.repository.StorageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class StorageProviderFactory {
    private final Map<Long, StorageProvider> cache = new ConcurrentHashMap<>();
    private final StorageRepository storageRepository;

    public StorageProvider getProvider(Storage storage) {
        return cache.computeIfAbsent(storage.getId(), id -> createStorageProvider(storage));
    }

    public StorageProvider getProvider(Long storageId) {
        StorageProvider storageProvider = cache.get(storageId);
        if (storageProvider == null) {
            Storage storage = storageRepository.findById(storageId)
                    .orElseThrow(() -> new ApiException(ErrorCode.STORAGE_NOT_FOUND));

            return cache.computeIfAbsent(storage.getId(), id -> createStorageProvider(storage));
        }

        return cache.get(storageId);
    }

    public void invalidate(Long storageId) {
        cache.remove(storageId);
    }

    public void invalidateAll() {
        cache.clear();
    }

    private StorageProvider createStorageProvider(Storage storage) {
        return switch (storage.getType()) {
//            case S3 -> new MinioStorageProvider(storage.getConfig());
            case LOCAL -> new LocalStorageProvider(storage.getConfig());
            default -> throw new IllegalArgumentException("Не поддерживаемый тип хранилища: "
                    + storage.getType());
        };
    }
}
