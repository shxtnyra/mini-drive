package com.example.minidrive.provider;

import com.example.minidrive.model.storage.StorageFile;

import java.io.InputStream;

public interface StorageProvider {

    StorageFile uploadFile(String key, InputStream inputStream, long size);

    InputStream getFileStream(String key);

    void delete(String key);

    boolean exists(String key);

    String initiateMultipartUpload(String key);

    /** Загружает часть и возвращает её идентификатор для завершения multipart-загрузки. */
    String uploadChunk(String key, String uploadId, int partNumber, InputStream inputStream, long size);

    StorageFile completeMultipartUpload(String key, String uploadId, int totalParts);

    void abortMultipartUpload(String key, String uploadId);
}