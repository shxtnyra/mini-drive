package com.example.minidrive.configuration;

import com.example.minidrive.entity.Storage;
import com.example.minidrive.enums.StorageType;
import com.example.minidrive.repository.StorageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class StorageBootstrapRunner implements ApplicationRunner {
    private final StorageRepository storageRepository;
    private final Clock clock;

    @Value("${app.storage.local.base-path}")
    private String basePath;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (storageRepository.getDefaultStorage().isPresent())
            return;

        Instant now = clock.instant();
        Map<String, Object> config = new HashMap<>();
        config.put("basePath", basePath + "/standard");

        Storage storage = Storage.builder()
                .name("Base")
                .type(StorageType.LOCAL)
                .isDefault(true)
                .config(config)
                .createdAt(now)
                .build();

        storageRepository.save(storage);
    }
}
