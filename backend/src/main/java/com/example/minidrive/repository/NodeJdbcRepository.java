package com.example.minidrive.repository;

import com.example.minidrive.dto.file.FileResponses;
import com.example.minidrive.enums.Context;
import com.example.minidrive.enums.NodeType;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class NodeJdbcRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    /** Выбирает поля DTO одним запросом, включая связанные данные владельца и родителя. */
    public List<FileResponses.Response> findFileResponsesWithFilters(
            Context context,
            Long userId,
            Long parentId,
            String nodeType,
            List<String> mimeTypes,
            Long createdBy,
            Instant fromDate,
            Instant toDate,
            String sortBy,
            String sortDirection,
            Object cursorValue,
            Long cursorId,
            int size
    ) {
        StringBuilder sql = new StringBuilder("""
            SELECT
                n.uuid AS uuid,
                n.name AS filename,
                EXISTS (
                    SELECT 1 FROM favorites f
                    WHERE f.node_id = n.id AND f.user_id = :userId
                ) AS favorite,
                n.storage_id AS storage_id,
                n.node_type AS file_type,
                n.mime_type AS mime_type,
                n.size_bytes AS size_bytes,
                parent.uuid AS parent_uuid,
                owner.uuid AS owner_uuid,
                n.deleted_at AS deleted_at,
                n.created_at AS created_at,
                n.updated_at AS updated_at
            FROM nodes n
            LEFT JOIN nodes parent ON parent.id = n.parent_id
            JOIN users owner ON owner.id = n.owner_id
            WHERE 1=1
            """);
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("userId", userId);

        appendContextFilter(sql, params, context, parentId);
        appendOwnerFilter(sql, params, createdBy);
        appendTypeAndMimeFilters(sql, params, nodeType, mimeTypes);
        appendDateFilters(sql, params, fromDate, toDate);

        String sortCol = getSortColumn(sortBy);
        boolean isAsc = "ASC".equalsIgnoreCase(sortDirection);

        appendKeysetPagination(sql, params, sortCol, isAsc, cursorValue, cursorId);
        appendOrderingAndLimit(sql, params, sortCol, isAsc, size);

        return jdbcTemplate.query(sql.toString(), params, this::mapResponse);
    }

    private FileResponses.Response mapResponse(ResultSet rs, int rowNumber) throws SQLException {
        return new FileResponses.Response(
                rs.getObject("uuid", UUID.class),
                rs.getString("filename"),
                rs.getBoolean("favorite"),
                rs.getObject("storage_id", Long.class),
                NodeType.valueOf(rs.getString("file_type")),
                rs.getString("mime_type"),
                rs.getObject("size_bytes", Long.class),
                rs.getObject("parent_uuid", UUID.class),
                rs.getObject("owner_uuid", UUID.class),
                toInstant(rs.getTimestamp("deleted_at")),
                toInstant(rs.getTimestamp("created_at")),
                toInstant(rs.getTimestamp("updated_at"))
        );
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private void appendContextFilter(StringBuilder sql, MapSqlParameterSource params, Context context, Long parentId) {
        switch (context) {
            case DRIVE -> {
                sql.append(" AND n.owner_id = :userId AND n.deleted_at IS NULL ");
                if (parentId == null) {
                    sql.append(" AND n.parent_id IS NULL ");
                } else {
                    sql.append(" AND n.parent_id = :parentId ");
                    params.addValue("parentId", parentId);
                }
            }
            case TRASH -> sql.append(" AND n.owner_id = :userId AND n.deleted_at IS NOT NULL ");
            case SHARED -> sql.append("""
                AND n.deleted_at IS NULL AND EXISTS (
                    SELECT 1 FROM shares s 
                    LEFT JOIN user_groups ug ON s.group_id = ug.group_id 
                    WHERE s.node_id = n.id AND s.deleted_at IS NULL 
                    AND (s.expires_at IS NULL OR s.expires_at > CURRENT_TIMESTAMP)
                    AND (s.recipient_id = :userId OR ug.user_id = :userId)
                )
            """);
            case FAVORITE -> sql.append("""
                AND n.deleted_at IS NULL AND EXISTS (
                    SELECT 1 FROM favorites f WHERE f.node_id = n.id AND f.user_id = :userId
                )
            """);
        }
    }

    private void appendOwnerFilter(StringBuilder sql, MapSqlParameterSource params, Long createdBy) {
        if (createdBy != null) {
            sql.append(" AND n.owner_id = :createdBy ");
            params.addValue("createdBy", createdBy);
        }
    }

    private void appendTypeAndMimeFilters(StringBuilder sql, MapSqlParameterSource params, String nodeType, List<String> mimeTypes) {
        if (nodeType != null && !nodeType.isBlank()) {
            sql.append(" AND n.node_type = :nodeType ");
            params.addValue("nodeType", nodeType);
        }
        if (mimeTypes != null && !mimeTypes.isEmpty()) {
            StringJoiner or = new StringJoiner(" OR ", " AND (", ") ");
            for (int i = 0; i < mimeTypes.size(); i++) {
                or.add("n.mime_type ILIKE :mt" + i);
                params.addValue("mt" + i, mimeTypes.get(i).replace("*", "%"));
            }
            sql.append(or);
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