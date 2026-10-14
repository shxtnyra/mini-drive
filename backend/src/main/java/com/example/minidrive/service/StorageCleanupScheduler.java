package com.example.minidrive.service;

import com.example.minidrive.entity.Node;
import com.example.minidrive.enums.UploadSessionStatus;
import com.example.minidrive.enums.NodeType;
import com.example.minidrive.repository.NodeRepository;
import com.example.minidrive.repository.UploadSessionJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class StorageCleanupScheduler {
    private static final int BATCH_SIZE = 50;
    private static final Duration TERMINAL_UPLOAD_METADATA_RETENTION = Duration.ofDays(7);
    private static final List<UploadSessionStatus> ACTIVE_UPLOAD_STATUSES = List.of(
            UploadSessionStatus.INITIALIZING,
            UploadSessionStatus.UPLOADING,
            UploadSessionStatus.FINALIZING
    );
    private static final List<UploadSessionStatus> TERMINAL_UPLOAD_STATUSES = List.of(
            UploadSessionStatus.COMPLETED,
            UploadSessionStatus.ABORTED
    );

    private final Clock clock;
    private final NodeRepository nodeRepository;
    private final UploadSessionJpaRepository uploadSessionRepository;
    private final FileUploadService fileUploadService;
    private final FileService fileService;

    @Value("${app.trash.retention:30d}")
    private Duration trashRetention;

    @Scheduled(fixedDelayString = "${app.cleanup.fixed-delay:1h}")
    public void cleanExpiredStorageData() {
        Instant now = clock.instant();
        cleanExpiredUploads(now);
        cleanExpiredTrash(now.minus(trashRetention));
        removeOldTerminalUploadMetadata(now.minus(TERMINAL_UPLOAD_METADATA_RETENTION));
    }

    private void cleanExpiredUploads(Instant now) {
        var expiredSessions = uploadSessionRepository.findExpired(
                ACTIVE_UPLOAD_STATUSES,
                now,
                PageRequest.of(0, BATCH_SIZE)
        );

        int processedSessions = 0;
        for (var session : expiredSessions) {
            try {
                fileUploadService.cleanupExpiredUpload(session.getSessionUuid());
                processedSessions++;
            } catch (RuntimeException exception) {
                log.error("Не удалось очистить сеанс загрузки с истекшим сроком действия {}", session.getSessionUuid(), exception);
            }
        }
        if (processedSessions > 0) {
            log.info("Обработано просроченных upload-сессий: {}", processedSessions);
        }
    }

    private void cleanExpiredTrash(Instant deletedBefore) {
        List<UUID> expiredFiles = nodeRepository.findExpiredTrashNodes(
                        NodeType.FILE,
                        deletedBefore,
                        PageRequest.of(0, BATCH_SIZE)
                )
                .stream()
                .map(Node::getUuid)
                .toList();

        int processedFiles = 0;
        for (UUID fileUuid : expiredFiles) {
            try {
                fileService.purgeExpiredTrashFile(fileUuid, deletedBefore);
                processedFiles++;
            } catch (RuntimeException exception) {
                log.error("Не удалось очистить файл с истекшим сроком действия {}", fileUuid, exception);
            }
        }
        if (processedFiles > 0) {
            log.info("Обработано просроченных файлов из корзины: {}", processedFiles);
        }
    }

    private void removeOldTerminalUploadMetadata(Instant expiresBefore) {
        int deleted = uploadSessionRepository.deleteOldTerminalSessions(
                TERMINAL_UPLOAD_STATUSES,
                expiresBefore
        );
        if (deleted > 0) {
            log.info("Удалено {} старых записей о сеансах загрузки", deleted);
        }
    }
}
