package com.example.minidrive.dto.file;

import com.example.minidrive.dto.common.CursorResponse;
import com.example.minidrive.entity.Node;
import com.example.minidrive.enums.NodeType;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class FileResponses {
    public record Response (
            UUID uuid,
            String filename,
            boolean isFavorite,
            Long storageId,
            NodeType fileType,
            String mimeType,
            Long sizeBytes,
            UUID parentUuid,
            UUID ownerUuid,
            Instant deletedAt,
            Instant createdAt,
            Instant updatedAt
            ) {
        public static Response fromEntity(Node node, boolean isFavorite) {
            return new Response(
                    node.getUuid(),
                    node.getName(),
                    isFavorite,
                    node.getStorage() != null ? node.getStorage().getId() : null,
                    node.getNodeType(),
                    node.getMimeType(),
                    node.getSizeBytes(),
                    node.getParent() != null ? node.getParent().getUuid() : null,
                    node.getOwner().getUuid(),
                    node.getDeletedAt(),
                    node.getCreatedAt(),
                    node.getUpdatedAt()
            );
        }

        // Полукостыль
        public static Response fromEntity(Node node) {
            return fromEntity(node, false);
        }
    }

    public record Summary(
            UUID uuid,
            String name,
            NodeType type,
            Long sizeBytes
    ) {
        public static Summary fromEntity(Node node) {
            return new Summary(
                    node.getUuid(),
                    node.getName(),
                    node.getNodeType(),
                    node.getSizeBytes()
            );
        }
    }

    public record UploadStatus(
            UUID sessionUuid,
            List<Integer> uploadedParts,
            int totalParts
    ) { }

    public record BeginUpload(
            UUID sessionUuid,
            long chunkSize,
            int totalParts
    ) {}

    public record DownloadResult(
            InputStream inputStream,
            String fileName,
            String mimeType,
            long sizeBytes
    ) {}

    public record BreadcrumbItem(
            UUID uuid,
            String name
    ) {
        public static BreadcrumbItem fromEntity(Node node) {
            return new BreadcrumbItem(
                    node.getUuid(),
                    node.getName()
            );
        }
    }

    public record FolderResponse(
            List<BreadcrumbItem> breadcrumbs,
            CursorResponse<Response, CursorResponse.CursorDto> files
    ) {}
}
