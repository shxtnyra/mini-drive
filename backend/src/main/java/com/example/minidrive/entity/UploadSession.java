package com.example.minidrive.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "upload_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadSession {
    @Id
    @Column(name = "session_uuid", nullable = false, updatable = false)
    private UUID sessionUuid;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "parent_folder_id")
    private Long parentFolderId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "storage_id", nullable = false)
    private Long storageId;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(name = "upload_id")
    private String uploadId;

    @Column(name = "chunk_size", nullable = false)
    private Long chunkSize;

    @Column(name = "total_parts", nullable = false)
    private Integer totalParts;

    @Column(name = "reserved_bytes", nullable = false)
    private Long reservedBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UploadSessionStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
