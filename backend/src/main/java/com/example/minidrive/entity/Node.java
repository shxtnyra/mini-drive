package com.example.minidrive.entity;

import com.example.minidrive.enums.NodeType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "nodes")
@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Node {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid = UUID.randomUUID();

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parent_id", nullable = false)
    private Node parent;

    @Enumerated(EnumType.STRING)
    @Column(name = "node_type", nullable = false)
    private NodeType nodeType;

    @Column(name = "mime_type")
    private String mimeType;

    @Builder.Default
    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes = 0L;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "storage_id", nullable = false)
    private Storage storage;

    @Column(name = "storage_key")
    private String storageKey;

    @Builder.Default
    @Column(name = "is_shared", nullable = false)
    private Boolean isShared = false;

    @Builder.Default
    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    @Builder.Default
    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
