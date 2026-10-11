package com.example.minidrive.service;

import com.example.minidrive.configuration.properties.ChunkUploadProperties;
import com.example.minidrive.dto.file.FileRequests;
import com.example.minidrive.dto.file.FileResponses;
import com.example.minidrive.entity.Node;
import com.example.minidrive.entity.Storage;
import com.example.minidrive.entity.UploadSession;
import com.example.minidrive.entity.UploadSessionStatus;
import com.example.minidrive.entity.User;
import com.example.minidrive.enums.ErrorCode;
import com.example.minidrive.enums.NodeType;
import com.example.minidrive.exception.ApiException;
import com.example.minidrive.exception.StorageException;
import com.example.minidrive.model.redis.ChunkPartInfo;
import com.example.minidrive.provider.StorageProvider;
import com.example.minidrive.provider.StorageProviderFactory;
import com.example.minidrive.repository.NodeRepository;
import com.example.minidrive.repository.StorageRepository;
import com.example.minidrive.repository.UploadSessionJpaRepository;
import com.example.minidrive.repository.UserRepository;
import com.example.minidrive.repository.redis.UploadSessionRepository;
import com.example.minidrive.security.JwtUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {
    private static final List<UploadSessionStatus> ACTIVE_STATUSES = List.of(
            UploadSessionStatus.INITIALIZING,
            UploadSessionStatus.UPLOADING,
            UploadSessionStatus.FINALIZING
    );

    private final ChunkUploadProperties properties;
    private final UserRepository userRepository;
    private final NodeRepository nodeRepository;
    private final StorageRepository storageRepository;
    private final UploadSessionJpaRepository uploadSessionRepository;
    private final StorageProviderFactory storageProviderFactory;
    private final QuotaService quotaService;
    private final UploadSessionRepository redisRepository;
    private final Clock clock;

    /** Создаёт сессию и резервирует квоту до начала передачи частей. */
    @Transactional
    public FileResponses.BeginUpload initiateUpload(JwtUserDetails jwtUser, FileRequests.BeginUpload request) {
        User owner = userRepository.findById(jwtUser.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        Node parent = null;
        if (request.parentUuid() != null) {
            parent = nodeRepository.findByUuid(request.parentUuid())
                    .orElseThrow(() -> new ApiException(ErrorCode.FOLDER_NOT_FOUND));
            if (parent.getNodeType() != NodeType.FOLDER) {
                throw new ApiException(ErrorCode.NOT_A_FOLDER);
            }
        }

        Long parentId = parent == null ? null : parent.getId();
        if (nodeRepository.existsNodeName(owner.getId(), parentId, request.fileName(), NodeType.FILE)) {
            throw new ApiException(ErrorCode.FILENAME_IN_FOLDER_EXISTS);
        }

        long chunkSize = getAndValidateChunkSize(request);
        long partCount = ((request.fileSize() - 1) / chunkSize) + 1;
        if (partCount > Integer.MAX_VALUE) {
            throw new ApiException(ErrorCode.INVALID_INPUT, Map.of("field", "fileSize"));
        }

        long quota = quotaService.getUserQuota(owner.getId());
        int reservationCreated = userRepository.reserveUploadQuota(
                owner.getId(),
                request.fileSize(),
                quota,
                properties.maxActiveUploads()
        );
        if (reservationCreated == 0) {
            long activeCount = uploadSessionRepository.countByOwnerIdAndStatusIn(owner.getId(), ACTIVE_STATUSES);
            if (activeCount >= properties.maxActiveUploads()) {
                throw new ApiException(ErrorCode.TOO_MANY_ACTIVE_UPLOADS);
            }
            throw new ApiException(ErrorCode.QUOTA_EXCEEDED);
        }

        Storage storage = storageRepository.getDefaultStorage()
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_SERVER_ERROR));
        UUID sessionUuid = UUID.randomUUID();
        Instant now = clock.instant();
        UploadSession session = UploadSession.builder()
                .sessionUuid(sessionUuid)
                .ownerId(owner.getId())
                .parentFolderId(parentId)
                .fileName(request.fileName())
                .fileSize(request.fileSize())
                .mimeType(request.mimeType())
                .storageId(storage.getId())
                .storageKey(generateStorageKey(storage, owner))
                .chunkSize(chunkSize)
                .totalParts((int) partCount)
                .reservedBytes(request.fileSize())
                .status(UploadSessionStatus.INITIALIZING)
                .createdAt(now)
                .expiresAt(now.plus(properties.sessionExpire()))
                .build();
        uploadSessionRepository.save(session);

        StorageProvider provider = storageProviderFactory.getProvider(storage);
        String uploadId = provider.initiateMultipartUpload(session.getStorageKey());
        session.setUploadId(uploadId);
        session.setStatus(UploadSessionStatus.UPLOADING);
        uploadSessionRepository.save(session);

        log.info("Создана upload-сессия {} для пользователя {}, размер резервирования {} байт",
                sessionUuid, owner.getId(), request.fileSize());
        return new FileResponses.BeginUpload(sessionUuid, chunkSize, (int) partCount);
    }

    /** Проверяет владельца сессии и параметры части до вызова storage. */
    @Transactional
    public void uploadChunk(
            UUID sessionUuid,
            int partNumber,
            MultipartFile chunk,
            JwtUserDetails jwtUser
    ) {
        UploadSession session = getOwnedSessionOrThrow(sessionUuid, jwtUser);
        if (session.getStatus() != UploadSessionStatus.UPLOADING) {
            throw new ApiException(ErrorCode.UPLOAD_SESSION_NOT_FOUND);
        }
        if (partNumber < 1 || partNumber > session.getTotalParts()) {
            throw new ApiException(ErrorCode.INVALID_INPUT, Map.of(
                    "partNumber", partNumber,
                    "totalParts", session.getTotalParts()
            ));
        }

        long expectedSize = partNumber < session.getTotalParts()
                ? session.getChunkSize()
                : session.getFileSize() - session.getChunkSize() * (session.getTotalParts() - 1L);
        if (chunk.isEmpty() || chunk.getSize() != expectedSize) {
            throw new ApiException(ErrorCode.INVALID_INPUT, Map.of(
                    "partNumber", partNumber,
                    "expectedSize", expectedSize,
                    "receivedSize", chunk.getSize()
            ));
        }

        try (InputStream inputStream = chunk.getInputStream()) {
            StorageProvider provider = storageProviderFactory.getProvider(session.getStorageId());
            String eTag = provider.uploadChunk(
                    session.getStorageKey(),
                    session.getUploadId(),
                    partNumber,
                    inputStream,
                    chunk.getSize()
            );
            redisRepository.addUploadedPart(sessionUuid, partNumber, eTag, properties.sessionExpire());
        } catch (java.io.IOException | StorageException exception) {
            log.error("Ошибка записи фрагмента {} для сессии {}", partNumber, sessionUuid, exception);
            throw new ApiException(ErrorCode.CHUNK_WRITE_ERROR);
        }
    }

    @Transactional
    public FileResponses.Response completeUpload(UUID sessionUuid, JwtUserDetails jwtUser) {
        UploadSession session = getOwnedSessionOrThrow(sessionUuid, jwtUser);
        if (session.getStatus() != UploadSessionStatus.UPLOADING) {
            throw new ApiException(ErrorCode.UPLOAD_SESSION_NOT_FOUND);
        }

        List<ChunkPartInfo> uploadedParts = redisRepository.getUploadedParts(sessionUuid);
        if (!hasAllPartsInOrder(uploadedParts, session.getTotalParts())) {
            throw new ApiException(ErrorCode.INCOMPLETE_UPLOAD, Map.of(
                    "expected", session.getTotalParts(),
                    "received", uploadedParts.size()
            ));
        }

        uploadSessionRepository.transition(
                sessionUuid,
                UploadSessionStatus.UPLOADING,
                UploadSessionStatus.FINALIZING
        );
        session.setStatus(UploadSessionStatus.FINALIZING);

        StorageProvider provider = storageProviderFactory.getProvider(session.getStorageId());
        long actualSize = provider.completeMultipartUpload(
                session.getStorageKey(),
                session.getUploadId(),
                session.getTotalParts()
        ).sizeBytes();

        // Не допускаем превышения квоты, если фактический размер больше резерва.
        if (actualSize > session.getReservedBytes()) {
            throw new ApiException(ErrorCode.QUOTA_EXCEEDED);
        }

        User owner = userRepository.findById(session.getOwnerId())
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        Node parent = session.getParentFolderId() == null
                ? null
                : nodeRepository.getReferenceById(session.getParentFolderId());
        Storage storage = storageRepository.getReferenceById(session.getStorageId());
        Instant now = clock.instant();

        Node node = Node.builder()
                .owner(owner)
                .name(session.getFileName())
                .sizeBytes(actualSize)
                .mimeType(session.getMimeType())
                .nodeType(NodeType.FILE)
                .storageKey(session.getStorageKey())
                .storage(storage)
                .parent(parent)
                .createdAt(now)
                .updatedAt(now)
                .build();
        nodeRepository.save(node);

        userRepository.consumeUploadReservation(
                owner.getId(),
                session.getReservedBytes(),
                actualSize
        );

        session.setReservedBytes(0L);
        session.setStatus(UploadSessionStatus.COMPLETED);
        uploadSessionRepository.save(session);
        redisRepository.delete(sessionUuid);

        log.info("Upload-сессия {} завершена для пользователя {}, фактический размер {} байт",
                sessionUuid, owner.getId(), actualSize);
        return FileResponses.Response.fromEntity(node);
    }

    @Transactional(readOnly = true)
    public FileResponses.UploadStatus getUploadStatus(UUID sessionUuid, JwtUserDetails jwtUser) {
        UploadSession session = getOwnedSessionOrThrow(sessionUuid, jwtUser);
        List<Integer> uploadedPartNumbers = redisRepository.getUploadedParts(sessionUuid)
                .stream()
                .map(ChunkPartInfo::partNumber)
                .toList();
        return new FileResponses.UploadStatus(session.getSessionUuid(), uploadedPartNumbers, session.getTotalParts());
    }

    @Transactional
    public void abortUpload(UUID sessionUuid, JwtUserDetails jwtUser) {
        UploadSession session = getOwnedSessionOrThrow(sessionUuid, jwtUser);
        abortActiveSession(session);
    }

    @Transactional
    public void cleanupExpiredUpload(UUID sessionUuid) {
        UploadSession session = uploadSessionRepository.findById(sessionUuid).orElse(null);
        if (session == null || !session.getExpiresAt().isBefore(clock.instant())) {
            return;
        }
        abortActiveSession(session);
    }

    private boolean hasAllPartsInOrder(List<ChunkPartInfo> parts, int totalParts) {
        if (parts.size() != totalParts) {
            return false;
        }
        for (int index = 0; index < totalParts; index++) {
            if (parts.get(index).partNumber() != index + 1) {
                return false;
            }
        }
        return true;
    }

    private void abortActiveSession(UploadSession session) {
        if (!ACTIVE_STATUSES.contains(session.getStatus())) {
            return;
        }

        // Условный переход сериализует abort с completeUpload и предотвращает удаление
        // уже завершённого объекта. Ошибка storage откатывает транзакцию для повтора.
        int transitioned = uploadSessionRepository.transitionFromActive(
                session.getSessionUuid(),
                ACTIVE_STATUSES,
                UploadSessionStatus.ABORTED
        );
        if (transitioned == 0) {
            return;
        }

        StorageProvider provider = storageProviderFactory.getProvider(session.getStorageId());
        if (session.getUploadId() != null) {
            provider.abortMultipartUpload(session.getStorageKey(), session.getUploadId());
        }

        if (provider.exists(session.getStorageKey())) {
            provider.delete(session.getStorageKey());
        }

        if (userRepository.releaseUploadQuota(session.getOwnerId(), session.getReservedBytes()) != 1) {
            throw new IllegalStateException("Upload reservation was not available to release");
        }
        redisRepository.delete(session.getSessionUuid());

        log.info("Upload-сессия {} отменена, владелец {}, освобождено {} байт резерва",
                session.getSessionUuid(), session.getOwnerId(), session.getReservedBytes());
    }

    /** Чужая сессия неотличима от отсутствующей, чтобы не раскрывать её существование. */
    private UploadSession getOwnedSessionOrThrow(UUID sessionUuid, JwtUserDetails jwtUser) {
        UploadSession session = uploadSessionRepository.findById(sessionUuid).orElse(null);
        if (session == null || jwtUser == null) {
            throw new ApiException(ErrorCode.UPLOAD_SESSION_NOT_FOUND);
        }
        if (!session.getOwnerId().equals(jwtUser.getId())) {
            log.warn("Пользователь {} попытался обратиться к upload-сессии другого владельца {}",
                    jwtUser.getId(), sessionUuid);
            throw new ApiException(ErrorCode.UPLOAD_SESSION_NOT_FOUND);
        }
        return session;
    }

    private long getAndValidateChunkSize(FileRequests.BeginUpload request) {
        Long requestedSize = request.requestedChunkSize();
        if (requestedSize == null) {
            return properties.defaultSize();
        }
        if (requestedSize < properties.minSize()) {
            throw new ApiException(ErrorCode.CHUNK_SIZE_TOO_SMALL, Map.of(
                    "min", properties.minSize(),
                    "requested", requestedSize
            ));
        }
        if (requestedSize > properties.maxSize()) {
            throw new ApiException(ErrorCode.CHUNK_SIZE_TOO_LARGE, Map.of(
                    "max", properties.maxSize(),
                    "requested", requestedSize
            ));
        }
        return requestedSize;
    }

    private String generateStorageKey(Storage storage, User owner) {
        return String.format("%d/%s/%s", storage.getId(), owner.getUuid(), UUID.randomUUID());
    }
}
