package com.example.minidrive.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;

import com.example.minidrive.entity.Node;
import com.example.minidrive.enums.NodeType;

public interface NodeRepository extends JpaRepository<Node, Long> {
    
    Optional<Node> findByUuid(UUID parentUuid);

    boolean existsByUuidAndOwner_IdAndDeletedAtIsNull(UUID uuid, Long ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT n FROM Node n
        WHERE n.uuid = :uuid
          AND n.owner.id = :ownerId
          AND n.deletedAt IS NULL
        """)
    Optional<Node> findOwnedLiveNodeForUpdate(
            @Param("uuid") UUID uuid,
            @Param("ownerId") Long ownerId
    );

    Optional<Node> findByUuidAndDeletedAtIsNull(UUID uuid);

    Optional<Node> findByUuidAndOwner_IdAndDeletedAtIsNull(UUID uuid, Long ownerId);

    List<Node> findAllByUuidInAndOwner_IdAndDeletedAtIsNotNull(List<UUID> uuids, Long ownerId);

    List<Node> findAllByParent_IdAndDeletedAtIsNullOrderByNameAsc(Long parentId);

    @Query(value = """
        WITH RECURSIVE ancestry(id, parent_id) AS (
            SELECT n.id, n.parent_id
            FROM nodes n
            WHERE n.uuid = :fileUuid
              AND n.deleted_at IS NULL

            UNION ALL

            SELECT parent.id, parent.parent_id
            FROM nodes parent
            JOIN ancestry child ON child.parent_id = parent.id
            WHERE parent.deleted_at IS NULL
        )
        SELECT EXISTS (
            SELECT 1 FROM ancestry WHERE id = :rootId
        )
        """, nativeQuery = true)
    boolean isLiveNodeInSubtree(@Param("rootId") Long rootId, @Param("fileUuid") UUID fileUuid);

    @Query("""
        SELECT n.id
        FROM Node n
        WHERE n.uuid = :folderUuid
          AND n.nodeType = :nodeType
        """)
    Optional<Long> findFolderIdByUuid(
            @Param("folderUuid") UUID folderUuid,
            @Param("nodeType") NodeType nodeType
    );

    @Query("SELECT n.id FROM Node n WHERE n.uuid = :uuid")
    Long findIdByUuid(@Param("uuid") UUID uuid);

    @Query("""
    SELECT CASE WHEN COUNT(n) > 0 THEN true ELSE false END
    FROM Node n
    WHERE n.owner.id = :ownerId
      AND n.deletedAt IS NULL
      AND (:parentId IS NULL AND n.parent.id IS NULL OR n.parent.id = :parentId)
      AND n.name = :fileName
      AND n.nodeType = :nodeType
    """)
    boolean existsNodeName(Long ownerId, Long parentId, String fileName, NodeType nodeType);

    @Query(value = """
    WITH RECURSIVE subtree_ids AS (
        SELECT id
        FROM nodes
        WHERE uuid IN (:uuids)

        UNION ALL

        SELECT n.id
        FROM nodes AS n
        INNER JOIN subtree_ids AS s ON n.parent_id = s.id
    )
    SELECT * FROM nodes
    WHERE id IN (SELECT id FROM subtree_ids)
    """, nativeQuery = true)
    List<Node> findAllSubtreeByRootUuids(@Param("uuids") List<UUID> uuids);


    @Query(value = """
        WITH RECURSIVE requested AS (
            SELECT n.uuid, n.id, n.parent_id, n.owner_id
            FROM nodes n
            WHERE n.uuid IN (:nodeUuids)
        ),
        ancestry(requested_uuid, id, parent_id, owner_id) AS (
            SELECT r.uuid, r.id, r.parent_id, r.owner_id
            FROM requested r
            UNION ALL
            SELECT a.requested_uuid, parent.id, parent.parent_id, parent.owner_id
            FROM ancestry a
            JOIN nodes parent ON parent.id = a.parent_id
        ),
        effective_access AS (
            SELECT
                r.uuid,
                CASE
                    WHEN BOOL_OR(a.owner_id = :userId) THEN 4
                    ELSE COALESCE(MAX(
                        CASE
                            WHEN s.recipient_id = :userId OR ug.user_id = :userId THEN
                                CASE s.permissions
                                    WHEN 'READ' THEN 1
                                    WHEN 'WRITE' THEN 2
                                    WHEN 'ALL' THEN 3
                                    ELSE 0
                                END
                            ELSE 0
                        END
                    ), 0)
                END AS access_level
            FROM requested r
            JOIN ancestry a ON a.requested_uuid = r.uuid
            LEFT JOIN shares s
                ON s.node_id = a.id
                AND s.deleted_at IS NULL
                AND (s.expires_at IS NULL OR s.expires_at > CURRENT_TIMESTAMP)
            LEFT JOIN user_groups ug
                ON ug.group_id = s.group_id AND ug.user_id = :userId
            GROUP BY r.uuid
        )
        SELECT uuid
        FROM effective_access
        WHERE access_level >= :requiredLevel
        """, nativeQuery = true)
    List<UUID> findUuidsWithAccessLevel(
            @Param("nodeUuids") List<UUID> nodeUuids,
            @Param("userId") Long userId,
            @Param("requiredLevel") int requiredLevel
    );

    @Query(value = """
    WITH RECURSIVE breadcrumb_path AS (
        SELECT n.id, n.uuid, n.name, n.parent_id, 1 as level
        FROM nodes n
        WHERE n.uuid = :folderUuid
          AND n.owner_id = :userId
          AND n.node_type = 'FOLDER'
          AND n.deleted_at IS NULL
    
        UNION ALL
    
        SELECT n.id, n.uuid, n.name, n.parent_id, bp.level + 1
        FROM nodes n
        INNER JOIN breadcrumb_path bp ON n.id = bp.parent_id
        WHERE n.owner_id = :userId
          AND n.node_type = 'FOLDER'
          AND n.deleted_at IS NULL
    )
    SELECT uuid, name FROM breadcrumb_path
    ORDER BY level DESC
    """, nativeQuery = true)
    List<Object[]> findBreadcrumbPath(@Param("folderUuid") UUID folderUuid, @Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Node n SET n.deletedAt = :now WHERE n.uuid IN :uuids")
    void markAsDeleted(@Param("uuids") List<UUID> uuids, Instant now);

    @Modifying
    @Query("UPDATE Node n SET n.deletedAt = NULL WHERE n.uuid IN :uuids")
    void unmarkAsDeleted(@Param("uuids") List<UUID> uuids);

    @Modifying
    @Query(value = """
    INSERT INTO favorites(user_id, node_id)
    SELECT :userId, n.id
    FROM nodes n
    WHERE n.uuid IN (:uuids)
    ON CONFLICT (user_id, node_id) DO NOTHING
    """, nativeQuery = true)
    int addToFavorites(@Param("uuids") List<UUID> uuids, @Param("userId") Long userId);

    @Modifying
    @Query(value = """
    DELETE FROM favorites f
    WHERE f.user_id = :userId
      AND f.node_id IN (SELECT n.id FROM nodes n WHERE n.uuid IN (:uuids))
    """, nativeQuery = true)
    int removeFromFavorites(@Param("uuids") List<UUID> uuids, @Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
    UPDATE nodes
    SET parent_id = :targetFolderId,
        updated_at = :updatedAt
    WHERE uuid IN (:uuids)
      AND owner_id = :ownerId
      AND deleted_at IS NULL
    """, nativeQuery = true)
    int moveNodes(
            @Param("uuids") List<UUID> uuids,
            @Param("targetFolderId") Long targetFolderId,
            @Param("ownerId") Long ownerId,
            @Param("updatedAt") Instant updatedAt
    );

    @Query(value = """
    WITH RECURSIVE moved_subtree(id) AS (
        SELECT n.id
        FROM nodes n
        WHERE n.uuid IN (:uuids)
          AND n.deleted_at IS NULL

        UNION

        SELECT child.id
        FROM nodes child
        JOIN moved_subtree parent ON child.parent_id = parent.id
    )
    SELECT EXISTS (
        SELECT 1
        FROM moved_subtree
        WHERE id = :targetFolderId
    )
    """, nativeQuery = true)
    boolean targetIsInsideMovedSubtree(
            @Param("uuids") List<UUID> uuids,
            @Param("targetFolderId") Long targetFolderId
    );

    @Query("""
    SELECT n FROM Node n
    WHERE n.nodeType = :nodeType
      AND n.deletedAt IS NOT NULL
      AND n.deletedAt <= :deletedBefore
    ORDER BY n.deletedAt, n.id
    """)
    List<Node> findExpiredTrashNodes(
            @Param("nodeType") NodeType nodeType,
            @Param("deletedBefore") Instant deletedBefore,
            Pageable pageable
    );

}
