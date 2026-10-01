package com.example.minidrive.service;

import com.example.minidrive.dto.storage.StorageRequests;
import com.example.minidrive.dto.storage.StorageResponses;
import com.example.minidrive.entity.Storage;
import com.example.minidrive.enums.ErrorCode;
import com.example.minidrive.exception.ApiException;
import com.example.minidrive.repository.StorageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StorageService {
    private final StorageRepository storageRepository;
    private final Clock clock;

    @Transactional
    public StorageResponses.Base createStorage(StorageRequests.Create request) {
        if (storageRepository.existsByName(request.name()))
            throw new ApiException(ErrorCode.STORAGE_EXISTS);

        Instant now = clock.instant();
        Storage storage = Storage.builder()
                .name(request.name())
                .type(request.type())
                .config(request.config())
                .createdAt(now)
                .build();

        return StorageResponses.Base.fromEntity(storageRepository.save(storage));
    }

    @Transactional(readOnly = true)
    public List<StorageResponses.Base> getAllStorages() {
        return storageRepository.findAll().stream()
                .map(StorageResponses.Base::fromEntity).toList();
    }

    @Transactional
    public void setAsDefault(Long storageId) {
        Storage storage = storageRepository.findById(storageId)
                .orElseThrow(() -> new ApiException(ErrorCode.STORAGE_NOT_FOUND));

        storageRepository.clearDefault();
        storage.setDefault(true);
    }

    @Transactional
    public void deleteStorage(Long storageId) {
        Storage storage = storageRepository.findById(storageId)
                .orElseThrow(() -> new ApiException(ErrorCode.STORAGE_NOT_FOUND));

        if (storageRepository.isStorageInUse(storageId)) {
            throw new ApiException(ErrorCode.STORAGE_IN_USE);
        }

        storageRepository.delete(storage);
    }
}
