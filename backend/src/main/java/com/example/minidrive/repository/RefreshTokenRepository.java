package com.example.minidrive.repository;

import com.example.minidrive.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revokedAt = :now WHERE r.user.id = :userId AND r.revokedAt IS NULL")
    void revokeAllByUserId(@Param("userId") Long userId, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revokedAt = :now WHERE r.tokenHash = :tokenHash AND r.revokedAt IS NULL")
    void revokeByTokenHash(@Param("tokenHash") String tokenHash, @Param("now") Instant now);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Query("""
        SELECT r FROM RefreshToken r
        WHERE r.user.id = :userId
          AND r.revokedAt IS NULL
          AND r.expiresAt > :now
        ORDER BY r.lastUsedAt DESC
    """)
    List<RefreshToken> findAllActiveByUserId(Long userId, Instant now);
}
