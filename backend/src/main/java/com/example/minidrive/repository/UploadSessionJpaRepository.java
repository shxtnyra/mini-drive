package com.example.minidrive.repository;

import com.example.minidrive.entity.UploadSession;
import com.example.minidrive.entity.UploadSessionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface UploadSessionJpaRepository extends JpaRepository<UploadSession, UUID> {
    long countByOwnerIdAndStatusIn(Long ownerId, List<UploadSessionStatus> statuses);

    @Query("""
        SELECT s FROM UploadSession s
        WHERE s.status IN :statuses AND s.expiresAt <= :now
        ORDER BY s.expiresAt
        """)
    List<UploadSession> findExpired(
            @Param("statuses") List<UploadSessionStatus> statuses,
            @Param("now") Instant now,
            Pageable pageable
    );

    @Modifying
    @Query("""
        UPDATE UploadSession s SET s.status = :newStatus
        WHERE s.sessionUuid = :sessionUuid AND s.status = :expectedStatus
        """)
    int transition(
            @Param("sessionUuid") UUID sessionUuid,
            @Param("expectedStatus") UploadSessionStatus expectedStatus,
            @Param("newStatus") UploadSessionStatus newStatus
    );

    /**
     * Меняет только ещё активную сессию, чтобы повторный abort/cleanup
     * не освободил один и тот же резерв дважды.
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE UploadSession s SET s.status = :newStatus
        WHERE s.sessionUuid = :sessionUuid AND s.status IN :activeStatuses
        """)
    int transitionFromActive(
            @Param("sessionUuid") UUID sessionUuid,
            @Param("activeStatuses") List<UploadSessionStatus> activeStatuses,
            @Param("newStatus") UploadSessionStatus newStatus
    );

    @Modifying
    @Transactional
    @Query("""
        DELETE FROM UploadSession s
        WHERE s.status IN :terminalStatuses AND s.expiresAt < :before
        """)
    int deleteOldTerminalSessions(
            @Param("terminalStatuses") List<UploadSessionStatus> terminalStatuses,
            @Param("before") Instant before
    );
}
