package com.example.minidrive.provider;

import com.example.minidrive.exception.StorageException;
import com.example.minidrive.model.storage.StorageFile;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Slf4j
public class LocalStorageProvider implements StorageProvider{
    private final Map<String, Object> config;

    private Path getBasePath() {
        String basePath = (String) config.getOrDefault("basePath", "./storage_data");
        return Paths.get(basePath);
    }

    @Override
    public StorageFile uploadFile(InputStream inputStream, String key, long size) {
        Path filePath = getBasePath().resolve(key);

        try {
            Files.createDirectories(filePath.getParent());
            long bytesWritten = Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            return new StorageFile(key, bytesWritten);
        } catch (IOException e) {
            throw new StorageException("Ошибка при загрузке в локальное хранилище");
        }
    }

    @Override
    public InputStream getFileStream(String key) {
        Path filePath = getBasePath().resolve(key);

        try {
            if (!Files.exists(filePath)) {
                throw new StorageException("Файл не найден: " + key);
            }
            return Files.newInputStream(filePath);
        } catch (IOException e) {
            throw new StorageException("Ошибка при скачивании из локального хранилища");
        }
    }

    @Override
    public void delete(String key) {
        Path filePath = getBasePath().resolve(key);

        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new StorageException("Ошибка при удалении файла из локального хранилища");
        }
    }

    @Override
    public boolean exists(String key) {
        Path filePath = getBasePath().resolve(key);
        return Files.exists(filePath);
    }

    @Override
    public String initiateMultipartUpload(String key) {
        String uploadId = UUID.randomUUID().toString();
        Path tempDir = getBasePath().resolve(".tmp").resolve(uploadId);
        try {
            Files.createDirectories(tempDir);
            return uploadId;
        } catch (IOException e) {
            throw new StorageException("Не удалось создать директорию для сессии загрузки: " + e.getMessage());
        }
    }

    @Override
    public String uploadChunk(String key, String uploadId, int partNumber, InputStream inputStream, long size) {
        Path chunkPath = getBasePath().resolve(".tmp").resolve(uploadId).resolve("part-" + partNumber);
        try {
            Files.copy(inputStream, chunkPath, StandardCopyOption.REPLACE_EXISTING);
            // Локальный провайдер не вычисляет ETag; маркер нужен для учёта загруженной части.
            return "parts_" + partNumber;
        } catch (IOException e) {
            throw new StorageException("Ошибка при записи чанка #" + partNumber + ": " + e.getMessage());
        }
    }

    @Override
    public StorageFile completeMultipartUpload(String key, String uploadId, int totalParts) {
        Path tempDir = getBasePath().resolve(".tmp").resolve(uploadId);
        Path targetPath = getBasePath().resolve(key);

        try {
            Files.createDirectories(targetPath.getParent());

            try (FileChannel targetChannel = FileChannel.open(targetPath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {

                for (int i = 1; i <= totalParts; i++) {
                    Path chunkPath = tempDir.resolve("part-" + i);
                    if (!Files.exists(chunkPath)) {
                        throw new StorageException("Отсутствует чанк #" + i + " при сборке файла");
                    }

                    try (FileChannel chunkChannel = FileChannel.open(chunkPath, StandardOpenOption.READ)) {
                        chunkChannel.transferTo(0, chunkChannel.size(), targetChannel);
                    }
                    Files.delete(chunkPath);
                }
            }

            Files.deleteIfExists(tempDir);

            long totalSizeBytes = Files.size(targetPath);
            return new StorageFile(key, totalSizeBytes);

        } catch (IOException e) {
            throw new StorageException("Ошибка при сборке итогового файла из чанков: " + e.getMessage());
        }
    }

    @Override
    public void abortMultipartUpload(String key, String uploadId) {
        Path tempDir = getBasePath().resolve(".tmp").resolve(uploadId);
        if (Files.exists(tempDir)) {
            try (Stream<Path> walk = Files.walk(tempDir)) {
                walk.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException exception) {
                                log.error("Не удалось удалить временный файл multipart-загрузки {}", path, exception);
                            }
                        });
            } catch (IOException e) {
                throw new StorageException("Ошибка при очистке временных файлов сессии: " + e.getMessage());
            }
        }
    }
}
