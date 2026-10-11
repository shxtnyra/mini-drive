package com.example.minidrive.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import com.example.minidrive.entity.Node;
import com.example.minidrive.enums.NodeType;

public interface NodeRepository extends JpaRepository<Node, Long> {
    
    Optional<Node> findByUuid(UUID parentUuid);

    @Query("SELECT n.id FROM Node n WHERE n.uuid = :uuid")
    Long findIdByUuid(@Param("uuid") UUID uuid);

    /** Возвращает узлы из набора UUID, на которые у пользователя есть требуемый уровень доступа. */
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
        WITH RECURSIVE node_hierarchy AS (
            SELECT
                id,
                parent_id,
                owner_id,
                1 AS level
            FROM nodes
            WHERE uuid = :nodeUuid AND deleted_at IS NULL

            UNION ALL

            SELECT
                n.id,
                n.parent_id,
                n.owner_id,
                nh.level + 1
            FROM nodes n
            INNER JOIN node_hierarchy nh ON n.id = nh.parent_id
            WHERE n.deleted_at IS NULL
        ),

        permission_levels AS (
            SELECT
                'READ' AS perm_name, 1 AS perm_value
            UNION ALL
            SELECT 'WRITE', 2
            UNION ALL
            SELECT 'ALL', 3
            UNION ALL
            SELECT 'OWNER', 4
        ),

        all_shares AS (
            SELECT
                s.permissions,
                pl.perm_value
            FROM shares s
            JOIN permission_levels pl ON s.permissions = pl.perm_name
            LEFT JOIN user_groups ug ON s.group_id = ug.group_id
            WHERE s.node_id IN (SELECT id FROM node_hierarchy)
              AND s.deleted_at IS NULL
              AND (s.expires_at IS NULL OR s.expires_at > NOW())
              AND (s.recipient_id = :userId OR ug.user_id = :userId)
        ),

        ownership_check AS (
            SELECT
                CASE
                    WHEN EXISTS (
                        SELECT 1 FROM node_hierarchy WHERE owner_id = :userId
                    ) THEN 4
                    ELSE 0
                END AS owner_level
        )
        
        SELECT COALESCE(
            GREATEST(
                (SELECT owner_level FROM ownership_check),
                (SELECT COALESCE(MAX(perm_value), 0) FROM all_shares)
            ),
            0
        ) AS access_level
        """, nativeQuery = true)
    Integer getUserAccessLevel(@Param("nodeUuid") UUID nodeUuid, @Param("userId") Long userId);

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

    @Modifying
    @Query("UPDATE Node n SET n.deletedAt = :now WHERE n.uuid IN :uuids")
    void markAsDeleted(@Param("uuids") List<UUID> uuids, Instant now);

    @Modifying
    @Query("UPDATE Node n SET n.deletedAt = NULL WHERE n.uuid IN :uuids")
    void unmarkAsDeleted(@Param("uuids") List<UUID> uuids);

    @Query("""
        SELECT CASE WHEN COUNT(n) > 0 THEN true ELSE false END
        FROM Node n
        WHERE n.uuid = :nodeUuid
          AND (n.owner.id = :userId)
    """)
    boolean hasAccess(UUID nodeUuid, Long userId);

    @Query("SELECT n FROM Node n WHERE n.uuid IN :uuids")
    List<Node> findAllByUuid(@Param("uuids") List<UUID> uuids);

    @Query("SELECT n FROM Node n WHERE n.parent.id = :id")
    List<Node> findAllByParentId(@Param("id") Long id);

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

    List<Node> findAllByUuidIn(List<UUID> uuids);

    /** Выбирает ограниченный batch файлов, срок хранения которых в корзине истёк. */
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

    /** Выбирает только пустые папки с истёкшим сроком хранения. */
    @Query("""
        SELECT n FROM Node n
        WHERE n.nodeType = :nodeType
          AND n.deletedAt IS NOT NULL
          AND n.deletedAt <= :deletedBefore
          AND NOT EXISTS (SELECT child.id FROM Node child WHERE child.parent = n)
        ORDER BY n.deletedAt, n.id
        """)
    List<Node> findExpiredEmptyTrashFolders(
            @Param("nodeType") NodeType nodeType,
            @Param("deletedBefore") Instant deletedBefore,
            Pageable pageable
    );

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
}
