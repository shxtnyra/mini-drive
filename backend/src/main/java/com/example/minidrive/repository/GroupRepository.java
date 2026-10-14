package com.example.minidrive.repository;

import com.example.minidrive.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupRepository extends JpaRepository<Group, Long> {
    boolean existsByName(String name);

    @Modifying
    @Query(value = """
    INSERT INTO user_groups (user_id, group_id)
    SELECT u.id, :groupId
    FROM users u
    WHERE u.id IN (:userIds)
    ON CONFLICT (user_id, group_id) DO NOTHING
    """, nativeQuery = true)
    int addMembers(@Param("groupId") Long groupId, @Param("userIds") List<Long> userIds);

    @Modifying
    @Query(value = """
    DELETE FROM user_groups 
    WHERE group_id = :groupId AND user_id IN (:userIds)
    """, nativeQuery = true)
    int removeMembers(@Param("groupId") Long groupId, @Param("userIds") List<Long> userIds);

    @Modifying
    @Query("UPDATE Group g SET g.membersCount = g.membersCount + 1 WHERE g.id = :groupId")
    void incrementMembersCount(@Param("groupId") Long groupId);

    @Modifying
    @Query("UPDATE Group g SET g.membersCount = g.membersCount - 1 WHERE g.id = :groupId")
    void decrementMembersCount(@Param("groupId") Long groupId);

    @Query(value = """
    SELECT g.* FROM groups g
    JOIN user_groups ug ON g.id = ug.group_id
    WHERE ug.user_id = :userId
    """, nativeQuery = true)
    List<Group> getUserGroups(@Param("userId") Long userId);

    @Query(value = """
    SELECT ug.user_id
    FROM user_groups ug
    WHERE ug.group_id = :groupId
    """, nativeQuery = true)
    List<Long> findMemberIdsByGroupId(Long groupId);

    @Query(value = """
    SELECT MAX(g.quota_bytes) 
    FROM user_groups ug
    JOIN groups g ON ug.group_id = g.id
    WHERE ug.user_id = :userId
    """, nativeQuery = true)
    Optional<Long> findMaxQuotaByUserId(@Param("userId") Long userId);

    Optional<Group> findByUuid(UUID uuid);

    List<Group> findAllByUuidIn(List<UUID> uuids);

    List<Group> findByNameContainingIgnoreCaseOrderByNameAscUuidAsc(String query, Pageable pageable);

    boolean existsByUuid(UUID groupUuid);
}
