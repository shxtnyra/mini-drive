package com.example.minidrive.service;

import com.example.minidrive.configuration.properties.ChunkUploadProperties;
import com.example.minidrive.dto.file.FileRequests;
import com.example.minidrive.dto.file.FileResponses;
import com.example.minidrive.entity.Node;
import com.example.minidrive.entity.Storage;
import com.example.minidrive.entity.User;
import com.example.minidrive.enums.ErrorCode;
import com.example.minidrive.enums.NodeType;
import com.example.minidrive.exception.ApiException;
import com.example.minidrive.model.redis.ChunkPartInfo;
import com.example.minidrive.model.redis.UploadSessionData;
import com.example.minidrive.model.storage.StorageFile;
import com.example.minidrive.provider.StorageProvider;
import com.example.minidrive.provider.StorageProviderFactory;
import com.example.minidrive.repository.NodeRepository;
import com.example.minidrive.repository.StorageRepository;
import com.example.minidrive.repository.redis.UploadSessionRepository;
import com.example.minidrive.repository.UserRepository;
import com.example.minidrive.security.JwtUserDetails;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FileUploadService {
    private final ChunkUploadProperties properties;

    private final UserRepository userRepository;
    private final NodeRepository nodeRepository;
    private final StorageRepository storageRepository;
    private final StorageProviderFactory storageProviderFactory;
    private final QuotaService quotaService;
    private final UploadSessionRepository redisRepository;
    private final Tika tika;
    private final Clock clock;

    @Transactional(readOnly = true)
    public FileResponses.BeginUpload initiateUpload(JwtUserDetails jwtUser, FileRequests.BeginUpload request) {
        User owner = userRepository.findById(jwtUser.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        long maxQuota = quotaService.getUserQuota(owner.getId());
        if (owner.getUsedBytes() + request.fileSize() > maxQuota) {
            throw new ApiException(ErrorCode.QUOTA_EXCEEDED);
        }

        Node parent = null;
        if (request.parentUuid() != null) {
            parent = nodeRepository.findByUuid(request.parentUuid())
                    .orElseThrow(() -> new ApiException(ErrorCode.FOLDER_NOT_FOUND));

            if (parent.getNodeType() != NodeType.FOLDER) {
                throw new ApiException(ErrorCode.NOT_A_FOLDER);
            }
        }

        Long parentId = parent != null ? parent.getId() : null;
        if (nodeRepository.existsNodeName(owner.getId(), parentId, request.fileName(), NodeType.FILE)) {
            throw new ApiException(ErrorCode.FILENAME_IN_FOLDER_EXISTS);
        }

        Storage storage = storageRepository.getDefaultStorage()
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_SERVER_ERROR));

        StorageProvider provider = storageProviderFactory.getProvider(storage);
        String storageKey = generateStorageKey(storage, owner, request.fileName());
        String uploadId = provider.initiateMultipartUpload(storageKey);


        long chunkSize = getAndValidChunkSize(request);
        int totalParts = (int) Math.ceil((double) request.fileSize() / chunkSize);
        UUID sessionUuid = UUID.randomUUID();

        UploadSessionData sessionData = new UploadSessionData(
                sessionUuid, owner.getId(), owner.getUuid(), parentId,
                request.fileName(), request.fileSize(), request.mimeType(), storage.getId(), storage.getType().toString(),
                storageKey, uploadId, chunkSize, totalParts
        );
        redisRepository.save(sessionData, properties.sessionExpire());

        return new FileResponses.BeginUpload(sessionUuid, chunkSize, totalParts);
    }

    //TODO: Из первого чанка Mime-Type броать с помощью Tika

    // Здесь намеренно нет проверки прав на владение
    public void uploadChunk(UUID sessionUuid, int partNumber, MultipartFile chunk) {
        UploadSessionData session = getSessionOrThrow(sessionUuid);

        if (chunk.getSize() > session.chunkSize() + 1024) {
            throw new ApiException(ErrorCode.CHUNK_SIZE_TOO_LARGE);
        }

        try (InputStream is = chunk.getInputStream()) {
            StorageProvider provider = storageProviderFactory.getProvider(session.storageId());

            String eTag = provider.uploadChunk(session.storageKey(), session.uploadId(), partNumber, is, chunk.getSize());
            redisRepository.addUploadedPart(sessionUuid, partNumber, eTag, properties.sessionExpire());
        } catch (Exception e) {
            throw new ApiException(ErrorCode.CHUNK_WRITE_ERROR);
        }
    }

    @Transactional
    public FileResponses.Response completeUpload(UUID sessionUuid) {
        UploadSessionData session = getSessionOrThrow(sessionUuid);
        List<ChunkPartInfo> uploadedParts = redisRepository.getUploadedParts(sessionUuid);

        if (uploadedParts.size() != session.totalParts()) {
            abortUpload(sessionUuid);
            throw new ApiException(ErrorCode.INCOMPLETE_UPLOAD,
                    Map.of("expected", session.totalParts(), "received", uploadedParts.size()));
        }

        StorageProvider provider = storageProviderFactory.getProvider(session.storageId());
        StorageFile storageFile = provider.completeMultipartUpload(
                session.storageKey(), session.uploadId(), session.totalParts()
        );

        long actualSize = storageFile.sizeBytes();
        Node node = saveNodeAndIncrementQuota(session, actualSize);

        redisRepository.delete(sessionUuid);
        return FileResponses.Response.fromEntity(node);
    }

    public FileResponses.UploadStatus getUploadStatus(UUID sessionUuid) {
        UploadSessionData session = getSessionOrThrow(sessionUuid);
        List<ChunkPartInfo> uploadedParts = redisRepository.getUploadedParts(sessionUuid);
        List<Integer> uploadPartsId = uploadedParts.stream().map(ChunkPartInfo::partNumber).toList();
        return new FileResponses.UploadStatus(session.sessionUuid(), uploadPartsId , session.totalParts());
    }

    public void abortUpload(UUID sessionUuid) {
        UploadSessionData session = redisRepository.findByUuid(sessionUuid);
        if (session != null) {
            StorageProvider provider = storageProviderFactory.getProvider(session.storageId());
            provider.abortMultipartUpload(session.storageKey(), session.uploadId());
            redisRepository.delete(sessionUuid);
        }
    }

    private Node saveNodeAndIncrementQuota(UploadSessionData session, long fileSize) {
        User owner = userRepository.findById(session.ownerId()).orElseThrow();
        long availableQuota = quotaService.getUserQuota(owner.getId()) - owner.getUsedBytes();

        if (fileSize > availableQuota) {
            storageProviderFactory.getProvider(session.storageId()).delete(session.storageKey());
            throw new ApiException(ErrorCode.QUOTA_EXCEEDED, "Фактический размер файла превысил квоту");
        }

        Node parentNode = null;
        if (session.parentFolderId() != null) {
            parentNode = nodeRepository.getReferenceById(session.parentFolderId());
        }
        Storage storage = storageRepository.getReferenceById(session.storageId());

        Instant now = clock.instant();
        Node node = Node.builder()
                .owner(owner)
                .name(session.fileName())
                .sizeBytes(fileSize)
                .mimeType(session.mimeType())
                .nodeType(NodeType.FILE)
                .storageKey(session.storageKey())
                .storage(storage)
                .parent(parentNode)
                .createdAt(now)
                .build();

        nodeRepository.save(node);
        userRepository.incrementUsedBytes(owner.getId(), fileSize);
        userRepository.save(owner);

        return node;
    }

    private long getAndValidChunkSize(FileRequests.BeginUpload request) {
        long chunkSize;
        Long requestedSize = request.requestedChunkSize();

        if (requestedSize == null) {
            chunkSize = properties.defaultSize();
        } else if (requestedSize < properties.minSize()) {
            throw new ApiException(ErrorCode.CHUNK_SIZE_TOO_SMALL, Map.of("min", properties.minSize(), "requested", requestedSize));
        } else if (requestedSize > properties.maxSize()) {
            throw new ApiException(ErrorCode.CHUNK_SIZE_TOO_LARGE, Map.of("max", properties.maxSize(), "requested", requestedSize));
        } else {
            chunkSize = requestedSize;
        }
        return chunkSize;
    }

    private UploadSessionData getSessionOrThrow(UUID sessionUuid) {
        UploadSessionData session = redisRepository.findByUuid(sessionUuid);
        if (session == null) {
            throw new ApiException(ErrorCode.UPLOAD_SESSION_NOT_FOUND);
        }
        return session;
    }

    private String generateStorageKey(Storage storage, User owner, String fileName) {
        // Формат: <storage_id>/<user_uuid>/<random_uuid>/<filename>
        return String.format("%d/%s/%s/%s",
                storage.getId(),
                owner.getUuid(),
                UUID.randomUUID(),
                fileName
        );
    }
}
