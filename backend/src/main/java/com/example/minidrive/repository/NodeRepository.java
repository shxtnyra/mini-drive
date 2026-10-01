package com.example.minidrive.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.minidrive.entity.Node;
import com.example.minidrive.enums.NodeType;

public interface NodeRepository extends JpaRepository<Node, Long> {
    
    Optional<Node> findByUuid(UUID parentUuid);

    @Query("SELECT n.id FROM Node n WHERE n.uuid = :uuid")
    Long findIdByUuid(@Param("uuid") UUID uuid);

    // Проверка доступа к ОДНОМУ узлу (учитывает владельца, прямой шеринг, шеринг группы И родительские папки)
    @Query(value = """
        WITH RECURSIVE node_hierarchy AS (
            -- Начинаем с целевого узла
            SELECT id, parent_id, owner_id FROM nodes WHERE uuid = :nodeUuid
            UNION ALL
            -- Поднимаемся вверх по иерархии к родителям
            SELECT n.id, n.parent_id, n.owner_id
            FROM nodes n
            INNER JOIN node_hierarchy nh ON n.id = nh.parent_id
        )
        -- Проверяем, есть ли среди текущего узла или его родителей тот, к которому у пользователя есть доступ
        SELECT EXISTS (
            SELECT 1 FROM node_hierarchy nh
            WHERE nh.owner_id = :userId
               OR EXISTS (SELECT 1 FROM shares s WHERE s.node_id = nh.id AND s.recipient_id = :userId AND s.deleted_at IS NULL)
               OR EXISTS (SELECT 1 FROM shares s 
                          JOIN user_groups ug ON s.group_id = ug.group_id 
                          WHERE s.node_id = nh.id AND ug.user_id = :userId AND s.deleted_at IS NULL)
        )
        """, nativeQuery = true)
    boolean hasAccessToNode(@Param("nodeUuid") UUID nodeUuid, @Param("userId") Long userId);

    // 2. МАССОВАЯ проверка: возвращает список UUID из запрошенных, к которым у пользователя ЕСТЬ доступ
    // Это позволяет избежать N+1 при проверке массива файлов
    @Query(value = """
        SELECT n.uuid FROM nodes n
        WHERE n.uuid IN (:nodeUuids)
        AND (
            n.owner_id = :userId
            OR EXISTS (SELECT 1 FROM shares s WHERE s.node_id = n.id AND s.recipient_id = :userId AND s.deleted_at IS NULL)
            OR EXISTS (SELECT 1 FROM shares s 
                       JOIN user_groups ug ON s.group_id = ug.group_id 
                       WHERE s.node_id = n.id AND ug.user_id = :userId AND s.deleted_at IS NULL)
        )
        """, nativeQuery = true)
    List<UUID> findAccessibleNodeUuids(@Param("nodeUuids") List<UUID> nodeUuids, @Param("userId") Long userId);

    // Примечание: Для массовой проверки мы пока не используем рекурсивный CTE для родителей,
    // так как это может быть тяжело для больших списков.
    // На практике, если пользователь видит файл в UI (через getFilesByContext), он уже имеет к нему доступ.
    // Эта проверка защищает от прямой подмены UUID в API. Если нужна 100% строгость для вложенных файлов,
    // можно использовать тот же CTE, но с `n.uuid IN (:nodeUuids)` в базовом случае.

    /*
        MAX(s.permissions) сработает, потому что в PostgreSQL строки сравниваются лексикографически.
        Если у тебя в enum порядок ALL > WRITE > READ,
        то MAX вернет самый высокий уровень.
     */
    @Query(value = """
        SELECT 
            CASE 
                -- 1. Если пользователь владелец, у него полные права (OWNER)
                WHEN n.owner_id = :userId THEN 'OWNER'
                
                -- 2. Иначе ищем максимальное право в таблице shares (для пользователя или его групп)
                ELSE (
                    SELECT MAX(s.permissions) 
                    FROM shares s
                    LEFT JOIN user_groups ug ON s.group_id = ug.group_id
                    WHERE s.node_id = n.id 
                      AND s.deleted_at IS NULL
                      AND (s.recipient_id = :userId OR ug.user_id = :userId)
                )
            END AS access_level
        FROM nodes n
        WHERE n.uuid = :nodeUuid
        """, nativeQuery = true)
    String getUserAccessLevel(@Param("nodeUuid") UUID nodeUuid, @Param("userId") Long userId);




    @Query(value = """
    SELECT n.* FROM nodes n 
    WHERE n.owner_id = :userId
      AND n.deleted_at IS NULL
      AND ((:parentId IS NULL AND n.parent_id IS NULL) OR n.parent_id = :parentId)
      AND (:cursorUuid IS NULL OR n.id < (SELECT c.id FROM nodes c WHERE c.uuid = :cursorUuid)) 
    ORDER BY n.id DESC
    LIMIT :size
    """, nativeQuery = true)
    List<Node> findNextPage(@Param("userId") Long userId,
                            @Param("cursorUuid") UUID cursorUuid,
                            @Param("parentId") Long parentId,
                            @Param("size") int size);

    // МОЙ ДИСК: Корень (parent_id IS NULL)
    @Query(value = """
        SELECT n.* FROM nodes n 
        WHERE n.owner_id = :userId 
          AND n.deleted_at IS NULL 
          AND n.parent_id IS NULL
          AND (:cursorId IS NULL OR n.id < :cursorId)
        ORDER BY n.id DESC 
        LIMIT :size
        """, nativeQuery = true)
    List<Node> findRootDriveNodes(@Param("userId") Long userId,
                                  @Param("cursorId") Long cursorId,
                                  @Param("size") int size);

    // МОЙ ДИСК: Вложенная папка
    @Query(value = """
        SELECT n.* FROM nodes n 
        WHERE n.owner_id = :userId 
          AND n.deleted_at IS NULL 
          AND n.parent_id = :parentId
          AND (:cursorId IS NULL OR n.id < :cursorId)
        ORDER BY n.id DESC 
        LIMIT :size
        """, nativeQuery = true)
    List<Node> findChildDriveNodes(@Param("userId") Long userId,
                                   @Param("parentId") Long parentId,
                                   @Param("cursorId") Long cursorId,
                                   @Param("size") int size);

    // КОРЗИНА
    @Query(value = """
        SELECT n.* FROM nodes n 
        WHERE n.owner_id = :userId 
          AND n.deleted_at IS NOT NULL
          AND (:cursorId IS NULL OR n.id < :cursorId)
        ORDER BY n.id DESC 
        LIMIT :size
        """, nativeQuery = true)
    List<Node> findTrashNodes(@Param("userId") Long userId,
                              @Param("cursorId") Long cursorId,
                              @Param("size") int size);

    // ОБЩИЙ ДОСТУП
    @Query(value = """
        SELECT n.* FROM nodes n 
        WHERE n.deleted_at IS NULL
          AND (:cursorId IS NULL OR n.id < :cursorId)
          AND EXISTS (
              SELECT 1 FROM shares s 
              LEFT JOIN user_groups ug ON s.group_id = ug.group_id 
              WHERE s.node_id = n.id 
                AND s.deleted_at IS NULL 
                AND (s.recipient_id = :userId OR ug.user_id = :userId)
          )
        ORDER BY n.id DESC 
        LIMIT :size
        """, nativeQuery = true)
    List<Node> findSharedNodes(@Param("userId") Long userId,
                               @Param("cursorId") Long cursorId,
                               @Param("size") int size);
    // ИЗБРАННОЕ
    @Query(value = """
        SELECT n.* FROM nodes n 
        WHERE n.deleted_at IS NULL
          AND (:cursorId IS NULL OR n.id < :cursorId)
          AND EXISTS (
              SELECT 1 FROM favorites f 
              WHERE f.node_id = n.id AND f.user_id = :userId
          )
        ORDER BY n.id DESC 
        LIMIT :size
        """, nativeQuery = true)
    List<Node> findFavoriteNodes(@Param("userId") Long userId,
                                 @Param("cursorId") Long cursorId,
                                 @Param("size") int size);
    // ПОИСК крошек
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

    @Query(value = """
    SELECT n.* FROM nodes n
    WHERE n.owner_id = :userId
      AND n.deleted_at IS NOT NULL
      AND (:cursorUuid IS NULL OR n.id < (SELECT c.id FROM nodes c WHERE c.uuid = :cursorUuid)) 
    ORDER BY n.id DESC
    LIMIT :size
    """, nativeQuery = true)
    List<Node> findNextPageTrash(@Param("userId") Long userId,
                                 @Param("cursorUuid") UUID cursorUuid,
                                 @Param("size") int size);

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
    @Query("UPDATE Node n SET n.deletedAt = :now WHERE n.uuid = :nodeUuid")
    void setDeletedAtByUuid(UUID nodeUuid, Instant now);

    @Modifying
    @Query("UPDATE Node n SET n.deletedAt = :now WHERE n.uuid IN :uuids")
    void markAsDeleted(@Param("uuids") List<UUID> uuids, Instant now);

    @Modifying
    @Query("UPDATE Node n SET n.deletedAt = NULL WHERE n.uuid = :nodeUuid")
    void restoreByUuid(UUID nodeUuid);

    @Modifying
    @Query("UPDATE Node n SET n.deletedAt = NULL WHERE n.uuid IN :uuids")
    void unmarkAsDeleted(@Param("uuids") List<UUID> uuids);

    @Query("""
        SELECT CASE WHEN COUNT(n) > 0 THEN true ELSE false END
        FROM Node n
        WHERE n.uuid = :nodeUuid 
          AND (n.owner.id = :userId 
               /* В будущем для расшаренных файлов просто раскомментируешь: */
               /* OR EXISTS (SELECT 1 FROM NodeShare s WHERE s.node = n AND s.user.id = :userId) */
              )
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

    @Query(value = """
    INSERT INTO favorites(user_id, node_id)
    VALUES(:userId, :fileId)
    """, nativeQuery = true)
    void makeFileFavorite(@Param("fileId") Long fileId, @Param("userId") Long userId);

    @Query(value = """
    DELETE FROM favorites(user_id, file_id)
    WHERE user_id = :userId AND node_id = :fileId
    """, nativeQuery = true)
    void makeFileUnfavorite(@Param("fileId") Long fileId, @Param("userId") Long userId);
}
