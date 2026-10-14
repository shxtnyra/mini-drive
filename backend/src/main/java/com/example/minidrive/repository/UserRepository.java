package com.example.minidrive.repository;

import com.example.minidrive.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String s);

    Optional<User> findByEmail(String email);

    Optional<User> findByUuid(UUID uuid);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    List<User> findAllByUuidIn(List<UUID> uuids);

    List<User> findAllByUuidInAndDeletedAtIsNull(List<UUID> uuids);

    @Query("SELECT u.id FROM User u WHERE u.uuid = :uuid")
    Long findIdByUuid(@Param("uuid") UUID uuid);


    @Query("""
        SELECT u FROM User u
        WHERE u.deletedAt IS NULL
          AND u.uuid <> :currentUserUuid
          AND (
              LOWER(COALESCE(u.displayName, '')) LIKE LOWER(CONCAT('%', :query, '%'))
              OR LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))
              OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))
          )
        ORDER BY LOWER(COALESCE(u.displayName, u.username)), u.uuid
        """)
    List<User> searchShareTargets(
            @Param("query") String query,
            @Param("currentUserUuid") UUID currentUserUuid,
            Pageable pageable
    );

    Page<User> findAll(Pageable pageable);

    @Query("""
    SELECT u FROM User u 
    WHERE (:lastId IS NULL OR u.id > :lastId) 
    ORDER BY u.id ASC
    LIMIT :limit
    """)
    List<User> findNextPage(@Param("lastId") Long lastId, @Param("limit") int limit);

    @Modifying
    @Query("UPDATE User u SET u.usedBytes = u.usedBytes + :size WHERE u.id = :id")
    void incrementUsedBytes(@Param("id") Long id, @Param("size") long size);

    @Modifying
    @Query("""
        UPDATE User u
        SET u.usedBytes = u.usedBytes - :size
        WHERE u.id = :id AND u.usedBytes >= :size
        """)
    int decrementUsedBytes(@Param("id") Long id, @Param("size") long size);

    @Modifying
    @Transactional
    @Query(value = """
        UPDATE users u
        SET reserved_bytes = u.reserved_bytes + :reservation,
            active_uploads = u.active_uploads + 1
        WHERE u.id = :userId
          AND u.used_bytes + u.reserved_bytes + :reservation <= :quota
          AND u.active_uploads < :maxActiveUploads
        """, nativeQuery = true)
    int reserveUploadQuota(
            @Param("userId") Long userId,
            @Param("reservation") long reservation,
            @Param("quota") long quota,
            @Param("maxActiveUploads") int maxActiveUploads
    );

    @Modifying
    @Transactional
    @Query("""
        UPDATE User u
        SET u.reservedBytes = u.reservedBytes - :reservation,
            u.activeUploads = u.activeUploads - 1
        WHERE u.id = :userId
          AND u.reservedBytes >= :reservation
          AND u.activeUploads > 0
        """)
    int releaseUploadQuota(@Param("userId") Long userId, @Param("reservation") long reservation);

    @Modifying
    @Transactional
    @Query("""
        UPDATE User u
        SET u.reservedBytes = u.reservedBytes - :reservation,
            u.usedBytes = u.usedBytes + :actualSize,
            u.activeUploads = u.activeUploads - 1
        WHERE u.id = :userId
          AND u.reservedBytes >= :reservation
          AND u.activeUploads > 0
        """)
    int consumeUploadReservation(
            @Param("userId") Long userId,
            @Param("reservation") long reservation,
            @Param("actualSize") long actualSize
    );

    @Query("SELECT u.activeUploads FROM User u WHERE u.id = :userId")
    int findActiveUploadCount(@Param("userId") Long userId);

    @Query(
    value = """
        SELECT u.* FROM users u
        JOIN user_groups ug ON ug.user_id = u.id
        WHERE ug.group_id = :groupId AND u.deletedAt IS NULL
    """,
    countQuery = """
        SELECT COUNT(*) FROM user_groups ug
        JOIN users u ON u.id = ug.user_id
        WHERE ug.group_id = :groupId AND u.deletedAt IS NULL
    """,
    nativeQuery = true
    )
    Page<User> findByGroup(@Param("groupId") Long groupId, Pageable pageable);
}