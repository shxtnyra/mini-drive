package com.example.minidrive.repository;

import com.example.minidrive.entity.Share;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShareRepository extends JpaRepository<Share, Long> {
    @Query("""
        SELECT s FROM Share s
        LEFT JOIN FETCH s.recipient
        LEFT JOIN FETCH s.group
        WHERE s.node.id = :nodeId
          AND s.deletedAt IS NULL
        """)
    List<Share> findActiveByNodeId(@Param("nodeId") Long nodeId);

    @Query("""
        SELECT s FROM Share s
        JOIN FETCH s.node
        WHERE s.shareToken = :token
          AND s.deletedAt IS NULL
        """)
    Optional<Share> findActiveLink(@Param("token") String token);

    Optional<Share> findByUuidAndDeletedAtIsNull(UUID uuid);
}
