package com.example.minidrive.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class NodeJdbcRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<UUID> findNodeUuidsWithFilters(
            String context,
            Long userId,
            Long parentId,
            String nodeType,
            String mimeType,
            Long createdBy,
            Instant fromDate,
            Instant toDate,
            String sortBy,
            String sortDirection,
            Object cursorValue,
            Long cursorId,
            int size
    ) {
        StringBuilder sql = new StringBuilder("SELECT n.uuid FROM nodes n WHERE 1=1 ");
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("userId", userId);

        // Сборка составных частей SQL-запроса
        appendContextFilter(sql, params, context, parentId);
        appendOwnerFilter(sql, params, createdBy);
        appendTypeAndMimeFilters(sql, params, nodeType, mimeType);
        appendDateFilters(sql, params, fromDate, toDate);

        String sortCol = getSortColumn(sortBy);
        boolean isAsc = "ASC".equalsIgnoreCase(sortDirection);

        appendKeysetPagination(sql, params, sortCol, isAsc, cursorValue, cursorId);
        appendOrderingAndLimit(sql, params, sortCol, isAsc, size);

        return jdbcTemplate.queryForList(sql.toString(), params, UUID.class);
    }

    private void appendContextFilter(StringBuilder sql, MapSqlParameterSource params, String context, Long parentId) {
        switch (context.toUpperCase()) {
            case "DRIVE" -> {
                sql.append(" AND n.owner_id = :userId AND n.deleted_at IS NULL ");
                if (parentId == null) {
                    sql.append(" AND n.parent_id IS NULL ");
                } else {
                    sql.append(" AND n.parent_id = :parentId ");
                    params.addValue("parentId", parentId);
                }
            }
            case "TRASH" -> sql.append(" AND n.owner_id = :userId AND n.deleted_at IS NOT NULL ");
            case "SHARED" -> sql.append("""
                AND n.deleted_at IS NULL AND EXISTS (
                    SELECT 1 FROM shares s 
                    LEFT JOIN user_groups ug ON s.group_id = ug.group_id 
                    WHERE s.node_id = n.id AND s.deleted_at IS NULL 
                    AND (s.recipient_id = :userId OR ug.user_id = :userId)
                )
            """);
            case "FAVORITE" -> sql.append("""
                AND n.deleted_at IS NULL AND EXISTS (
                    SELECT 1 FROM favorites f WHERE f.node_id = n.id AND f.user_id = :userId
                )
            """);
        }
    }

    public Set<Long> getFavoriteNodeIds(Long userId, List<Long> nodeIds) {
        if (nodeIds.isEmpty()) {
            return Collections.emptySet();
        }

        String sql = """
            SELECT node_id 
            FROM favorites 
            WHERE user_id = :userId 
            AND node_id IN (:nodeIds)
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("nodeIds", nodeIds);

        return new HashSet<>(jdbcTemplate.queryForList(sql, params, Long.class));
    }

    private void appendOwnerFilter(StringBuilder sql, MapSqlParameterSource params, Long createdBy) {
        if (createdBy != null) {
            sql.append(" AND n.owner_id = :createdBy ");
            params.addValue("createdBy", createdBy);
        }
    }

    private void appendTypeAndMimeFilters(StringBuilder sql, MapSqlParameterSource params, String nodeType, String mimeType) {
        if (nodeType != null && !nodeType.isBlank()) {
            sql.append(" AND n.node_type = :nodeType ");
            params.addValue("nodeType", nodeType);
        }
        if (mimeType != null && !mimeType.isBlank()) {
            sql.append(" AND n.mime_type LIKE :mimeType ");
            params.addValue("mimeType", mimeType.replace("*", "%"));
        }
    }

    private void appendDateFilters(StringBuilder sql, MapSqlParameterSource params, Instant fromDate, Instant toDate) {
        if (fromDate != null) {
            sql.append(" AND n.created_at >= :fromDate ");
            params.addValue("fromDate", Timestamp.from(fromDate));
        }
        if (toDate != null) {
            sql.append(" AND n.created_at <= :toDate ");
            params.addValue("toDate", Timestamp.from(toDate));
        }
    }

    private String getSortColumn(String sortBy) {
        return switch (sortBy != null ? sortBy : "") {
            case "name" -> "n.name";
            case "size" -> "n.size_bytes";
            case "updatedAt" -> "COALESCE(n.updated_at, n.created_at)";
            default -> "n.created_at";
        };
    }

    private void appendKeysetPagination(StringBuilder sql, MapSqlParameterSource params, String sortCol, boolean isAsc, Object cursorValue, Long cursorId) {
        if (cursorValue != null && cursorId != null) {
            Object formattedVal = (cursorValue instanceof Instant inst) ? Timestamp.from(inst) : cursorValue;
            String op = isAsc ? ">" : "<";

            sql.append(String.format("""
                AND (
                    %1$s %2$s :cursorValue 
                    OR (%1$s = :cursorValue AND n.id %2$s :cursorId)
                )
            """, sortCol, op));

            params.addValue("cursorValue", formattedVal);
            params.addValue("cursorId", cursorId);
        }
    }

    private void appendOrderingAndLimit(StringBuilder sql, MapSqlParameterSource params, String sortCol, boolean isAsc, int size) {
        String dir = isAsc ? "ASC" : "DESC";
        sql.append(String.format(" ORDER BY %s %s, n.id %s LIMIT :limit ", sortCol, dir, dir));
        params.addValue("limit", size + 1);
    }
}