package com.example.minidrive.repository;

import com.example.minidrive.entity.User;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String s);

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailOrUsername(String email, String username);

    Optional<User> findByUuid(UUID uuid);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    List<User> findAllByUuidIn(List<UUID> uuids);

    Page<User> findAll(Pageable pageable);

    @Query("""
    SELECT u FROM User u 
    WHERE (:lastId IS NULL OR u.id > :lastId) 
    ORDER BY u.id ASC
    LIMIT :limit
    """)
    List<User> findNextPage(@Param("lastId") Long lastId, @Param("limit") int limit);

    @Query("""
    SELECT u FROM User u
    WHERE u.email LIKE CONCAT('%', :query, '%')
    LIMIT 8
    """)
    List<User> searchUsersByEmail(@Param("query") String query);

    @Query("""
    SELECT u FROM User u
    WHERE u.username LIKE CONCAT('%', :query, '%')
    LIMIT 8
    """)
    List<User> searchUsersByUsername(@Param("query") String query);

    @Modifying
    @Query("UPDATE User u SET u.usedBytes = u.usedBytes + :size WHERE u.id = :id")
    void incrementUsedBytes(@Param("id") Long id, @Param("size") long size);

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

    @Query("SELECT u.id FROM User u WHERE u.uuid = :uuid")
    Long findIdByUuid(@Param("uuid") UUID uuid);
}