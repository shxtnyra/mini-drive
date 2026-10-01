package com.example.minidrive.model.redis;

import java.util.UUID;

public record UploadSessionData(
        UUID sessionUuid,
        Long ownerId,
        UUID ownerUuid,
        Long parentFolderId,
        String fileName,
        Long fileSize,
        String mimeType,
        Long storageId,
        String storageType,
        String storageKey,
        String uploadId,
        Long chunkSize,
        Integer totalParts
) {
}
