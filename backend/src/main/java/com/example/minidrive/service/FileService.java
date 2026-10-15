package com.example.minidrive.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.example.minidrive.enums.Context;
import com.example.minidrive.provider.StorageProvider;
import com.example.minidrive.provider.StorageProviderFactory;
import com.example.minidrive.repository.FileVersionRepository;
import com.example.minidrive.repository.NodeJdbcRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.minidrive.dto.common.CursorResponse;
import com.example.minidrive.dto.file.FileRequests;
import com.example.minidrive.dto.file.FileResponses;
import com.example.minidrive.entity.FileVersion;
import com.example.minidrive.entity.Node;
import com.example.minidrive.entity.User;
import com.example.minidrive.enums.ErrorCode;
import com.example.minidrive.enums.NodeType;
import com.example.minidrive.exception.ApiException;
import com.example.minidrive.repository.NodeRepository;
import com.example.minidrive.repository.UserRepository;
import com.example.minidrive.security.JwtUserDetails;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {
    private final NodeRepository nodeRepository;
    private final FileVersionRepository fileVersionRepository;
    private final NodeJdbcRepository nodeJdbcRepository;
    private final UserRepository userRepository;
    private final StorageProviderFactory storageProviderFactory;
    private final Clock clock;

    @Transactional(readOnly = true)
    @PreAuthorize("@fileSecurity.hasAccess(#parentUuid, #jwtUser)")
    public FileResponses.FolderResponse getFilesByContext(
            Context context,
            UUID parentUuid,
            String nodeType,
            List<String> mimeTypes,
            UUID createdBy,
            Instant fromDate,
            Instant toDate,
            String sortBy,
            String sortDirection,
            String cursorValue,
            UUID cursorUuid,
            int size,
            JwtUserDetails jwtUser
    ) {
        Long parentId = (parentUuid != null) ? nodeRepository.findIdByUuid(parentUuid) : null;
        Long createdById = (createdBy != null) ? userRepository.findIdByUuid(createdBy) : null;
        Long cursorId = (cursorUuid != null) ? nodeRepository.findIdByUuid(cursorUuid) : null;
        Object parsedCursorValue = parseCursorValue(cursorValue, sortBy);

        List<FileResponses.Response> filesResponse = nodeJdbcRepository.findFileResponsesWithFilters(
                context, jwtUser.getId(), parentId,
                nodeType, mimeTypes, createdById, fromDate, toDate,
                sortBy, sortDirection, parsedCursorValue, cursorId, size
        );

        List<FileResponses.BreadcrumbItem> breadcrumbs = buildBreadcrumbs(parentUuid, jwtUser.getId());

        if (filesResponse.isEmpty()) {
            return new FileResponses.FolderResponse(
                    breadcrumbs,
                    CursorResponse.of()
            );
        }

        CursorResponse<FileResponses.Response, CursorResponse.CursorDto> cursorPage = CursorResponse.of(
                filesResponse,
                size,
                file -> extractCursor(file, sortBy)
        );

        return new FileResponses.FolderResponse(breadcrumbs, cursorPage);
    }

    @Transactional()
    @PreAuthorize("@fileSecurity.canModify(#fileUuid, #jwtUser)")
    public FileResponses.Response updateFile(
            UUID fileUuid,
            FileRequests.UpdateFile request,
            JwtUserDetails jwtUser
    ) {
        Node file = nodeRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new ApiException(ErrorCode.FILE_NOT_FOUND));

        if (request.newFileName() != null && !request.newFileName().trim().isBlank()) {
            file.setName(request.newFileName());
        }

        return FileResponses.Response.fromEntity(file);
    }

    @Transactional
    @PreAuthorize("@fileSecurity.canModify(#request.parentUuid(), #jwtUser)")
    public FileResponses.Response createFolder(FileRequests.CreateFolder request, JwtUserDetails jwtUser) {
        Node parent = getParentFolder(request.parentUuid());
        User owner = userRepository.getReferenceById(jwtUser.getId());

        Instant now = clock.instant();

        Node node = Node.builder()
                .name(request.name())
                .owner(owner)
                .nodeType(NodeType.FOLDER)
                .parent(parent)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Node nodeSaved = nodeRepository.save(node);
        return FileResponses.Response.fromEntity(nodeSaved);
    }

    @Transactional
    @PreAuthorize("@fileSecurity.canDeleteAll(#request.fileUuids(), #jwtUser)")
    public void deleteFile(FileRequests.BatchNodeRequest request, JwtUserDetails jwtUser) {
        Instant now = clock.instant();
        nodeRepository.markAsDeleted(request.fileUuids(), now);
    }

    @Transactional
    @PreAuthorize("@fileSecurity.canDeleteAll(#request.fileUuids(), #jwtUser)")
    public void restoreFile(FileRequests.BatchNodeRequest request, JwtUserDetails jwtUser) {
        nodeRepository.unmarkAsDeleted(request.fileUuids());
    }

    @Transactional
    public void permanentlyDeleteFiles(FileRequests.BatchNodeRequest request, JwtUserDetails jwtUser) {
        List<UUID> rootUuids = request.fileUuids();
        if (rootUuids.stream().distinct().count() != rootUuids.size()) {
            throw new ApiException(ErrorCode.INVALID_INPUT);
        }

        List<Node> trashRoots = nodeRepository.findAllByUuidInAndOwner_IdAndDeletedAtIsNotNull(
                rootUuids,
                jwtUser.getId()
        );
        if (trashRoots.size() != rootUuids.size()) {
            throw new ApiException(ErrorCode.FILE_NOT_FOUND);
        }

        Map<Long, Node> subtreeById = nodeRepository.findAllSubtreeByRootUuids(rootUuids).stream()
                .collect(Collectors.toMap(Node::getId, node -> node, (first, ignored) -> first));
        List<Long> nodeIds = new ArrayList<>(subtreeById.keySet());
        List<FileVersion> versions = fileVersionRepository.findAllByNode_IdIn(nodeIds);

        for (FileVersion version : versions) {
            storageProviderFactory.getProvider(version.getStorage()).delete(version.getStorageKey());
        }
        for (Node node : subtreeById.values()) {
            if (node.getNodeType() == NodeType.FILE) {
                storageProviderFactory.getProvider(node.getStorage()).delete(node.getStorageKey());
            }
        }

        fileVersionRepository.deleteAllByNode_IdIn(nodeIds);

        Map<Long, Long> bytesByOwner = new HashMap<>();
        for (Node node : subtreeById.values()) {
            if (node.getNodeType() == NodeType.FILE) {
                bytesByOwner.merge(node.getOwner().getId(), node.getSizeBytes(), Long::sum);
            }
        }

        Map<Long, Integer> depths = new HashMap<>();
        subtreeById.values().stream()
                .sorted(Comparator.comparingInt((Node node) -> getSubtreeDepth(node, subtreeById, depths)).reversed())
                .forEach(node -> {
                    nodeRepository.delete(node);
                    nodeRepository.flush();
                });

        for (Map.Entry<Long, Long> entry : bytesByOwner.entrySet()) {
            if (userRepository.decrementUsedBytes(entry.getKey(), entry.getValue()) != 1) {
                throw new IllegalStateException("Не удалось обновить квоту после окончательного удаления файлов.");
            }
        }
    }

    private int getSubtreeDepth(Node node, Map<Long, Node> subtreeById, Map<Long, Integer> depths) {
        Integer knownDepth = depths.get(node.getId());
        if (knownDepth != null) {
            return knownDepth;
        }

        Node parent = node.getParent();
        int depth = parent != null && subtreeById.containsKey(parent.getId())
                ? getSubtreeDepth(subtreeById.get(parent.getId()), subtreeById, depths) + 1
                : 0;
        depths.put(node.getId(), depth);
        return depth;
    }

    @Transactional
    @PreAuthorize("@fileSecurity.canModifyAll(#request.fileUuids(), #jwtUser)" +
            " and @fileSecurity.canModify(#request.targetFolder(), #jwtUser)")
    public void moveFiles(FileRequests.MoveFiles request, JwtUserDetails jwtUser) {
        Long targetFolderId = request.targetFolder() == null
                ? null
                : nodeRepository.findFolderIdByUuid(request.targetFolder(), NodeType.FOLDER)
                        .orElseThrow(() -> new ApiException(ErrorCode.FOLDER_NOT_FOUND));


        List<UUID> fileUuids = request.fileUuids();

        if (targetFolderId != null
                && nodeRepository.targetIsInsideMovedSubtree(fileUuids, targetFolderId)) {
            throw new ApiException(ErrorCode.INVALID_INPUT,
                    Map.of("reason", "Целевая папка находится внутри перемещаемой структуры"));
        }

        int movedCount = nodeRepository.moveNodes(fileUuids, targetFolderId, jwtUser.getId(), clock.instant());
        if (movedCount != fileUuids.size()) {
            throw new ApiException(ErrorCode.FILE_NOT_FOUND);
        }
    }

    @Transactional(readOnly = true)
    @PreAuthorize(value = "@fileSecurity.hasAccess(#fileUuid, #jwtUser)")
    public FileResponses.DownloadResult downloadFile(UUID fileUuid, JwtUserDetails jwtUser) {
        Node file = nodeRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new ApiException(ErrorCode.FILE_NOT_FOUND));

        StorageProvider provider = storageProviderFactory.getProvider(file.getStorage());
        InputStream is = provider.getFileStream(file.getStorageKey());

        return new FileResponses.DownloadResult(
                is,
                file.getName(),
                file.getMimeType() != null && !file.getMimeType().isEmpty() ? file.getMimeType() : "application/octet-stream",
                file.getSizeBytes()
        );
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@fileSecurity.canReadAll(#request.fileUuids(), #jwtUser)")
    public void validateZipAccess(FileRequests.BatchNodeRequest request, JwtUserDetails jwtUser) {
        if (request == null || request.fileUuids() == null || request.fileUuids().isEmpty()) {
            throw new ApiException(ErrorCode.FILE_NOT_FOUND);
        }

        // Проверка выполняется до запуска асинхронной передачи ZIP-архива.
    }

    @Transactional(readOnly = true)
    public Path createZipArchive(FileRequests.BatchNodeRequest request) {
        Path archive = null;
        try {
            archive = Files.createTempFile("mini-drive-download-", ".zip");
            try (ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(archive))) {
                zipOut.setLevel(java.util.zip.Deflater.NO_COMPRESSION);
                streamZipFiles(request, zipOut);
            }
            return archive;
        } catch (IOException | RuntimeException exception) {
            log.error("Ошибка при создании ZIP для узлов {}", request.fileUuids(), exception);
            if (archive != null) {
                try {
                    Files.deleteIfExists(archive);
                } catch (IOException cleanupException) {
                    exception.addSuppressed(cleanupException);
                }
            }
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    @PreAuthorize(value = "@fileSecurity.canReadAll(#request.fileUuids(), #jwtUser)")
    public void addToFavorites(FileRequests.BatchNodeRequest request, JwtUserDetails jwtUser) {
        nodeRepository.addToFavorites(request.fileUuids(), jwtUser.getId());
    }

    @Transactional
    @PreAuthorize(value = "@fileSecurity.canReadAll(#request.fileUuids(), #jwtUser)")
    public void removeFromFavorites(FileRequests.BatchNodeRequest request, JwtUserDetails jwtUser) {
        nodeRepository.removeFromFavorites(request.fileUuids(), jwtUser.getId());
    }

    @Transactional
    public void purgeExpiredTrashFile(UUID nodeUuid, Instant deletedBefore) {
        Node file = nodeRepository.findByUuid(nodeUuid).orElse(null);
        if (file == null
                || file.getNodeType() != NodeType.FILE
                || file.getDeletedAt() == null
                || file.getDeletedAt().isAfter(deletedBefore)) {
            return;
        }

        storageProviderFactory.getProvider(file.getStorage()).delete(file.getStorageKey());
        nodeRepository.delete(file);
        if (userRepository.decrementUsedBytes(file.getOwner().getId(), file.getSizeBytes()) != 1) {
            throw new IllegalStateException("Не удалось обновить квоту после окончательного удаления файла.");
        }
    }

    private void streamZipFiles(FileRequests.BatchNodeRequest request, ZipOutputStream zipOut) {
        List<Node> allNodesInTree = nodeRepository.findAllSubtreeByRootUuids(request.fileUuids());

        Map<Long, List<Node>> childrenMap = allNodesInTree.stream()
                .filter(n -> n.getParent() != null)
                .collect(Collectors.groupingBy(n -> n.getParent().getId()));

        List<Node> rootNodes = allNodesInTree.stream()
                .filter(n -> request.fileUuids().contains(n.getUuid()))
                .toList();

        for (Node rootNode : rootNodes) {
            compressNodeRecursive(rootNode, "", zipOut, childrenMap);
        }
    }

    private void compressNodeRecursive(Node file, String parentPath, ZipOutputStream zipOut,  Map<Long, List<Node>> childrenMap) {
        String currentPath = parentPath.isEmpty()
                ? file.getName()
                : parentPath + "/" + file.getName();

        if (file.getNodeType() == NodeType.FOLDER) {
            ZipEntry folderEntry = new ZipEntry(currentPath + "/");

            try {
                zipOut.putNextEntry(folderEntry);
                zipOut.closeEntry();
            } catch (IOException e) {
                throw new RuntimeException("Ошибка при записи папки в ZIP", e);
            }

            List<Node> children = childrenMap.getOrDefault(file.getId(), Collections.emptyList());
            for (Node child : children) {
                compressNodeRecursive(child, currentPath, zipOut, childrenMap);
            }
        } else {
            ZipEntry fileEntry = new ZipEntry(currentPath);
            try {
                zipOut.putNextEntry(fileEntry);

                StorageProvider storageProvider = storageProviderFactory.getProvider(file.getStorage().getId());

                try (InputStream inputStream = storageProvider.getFileStream(file.getStorageKey())) {
                    inputStream.transferTo(zipOut);
                }

                zipOut.closeEntry();
            } catch (IOException e) {
                throw new RuntimeException("Ошибка при добавлении файла в ZIP", e);
            }
        }
    }

    private Node getParentFolder(UUID parentUuid) {
        Node parent = parentUuid != null
                ? nodeRepository.findByUuid(parentUuid)
                .orElseThrow(() -> new ApiException(ErrorCode.FILE_NOT_FOUND))
                : null;
        if (parent != null && parent.getNodeType().equals(NodeType.FILE)) {
            throw new ApiException(ErrorCode.NOT_A_FOLDER);
        }

        return parent;
    }

    private Object parseCursorValue(String cursorValue, String sortBy) {
        if (cursorValue == null || cursorValue.isBlank()) return null;

        return switch (sortBy != null ? sortBy : "") {
            case "size" -> Long.parseLong(cursorValue);
            case "updatedAt", "createdAt" -> Instant.parse(cursorValue);
            default -> cursorValue;
        };
    }

    private CursorResponse.CursorDto extractCursor(FileResponses.Response file, String sortBy) {
        if (file == null) return null;

        String value = switch (sortBy != null ? sortBy : "") {
            case "name" -> file.filename();
            case "size" -> String.valueOf(file.sizeBytes());
            case "updatedAt" -> file.updatedAt() != null ? file.updatedAt().toString() : file.createdAt().toString();
            default -> file.createdAt().toString();
        };

        return new CursorResponse.CursorDto(value, file.uuid());
    }

    private List<FileResponses.BreadcrumbItem> buildBreadcrumbs(UUID folderUuid, Long userId) {
        if (folderUuid == null) {
            return List.of();
        }

        List<Object[]> results = nodeRepository.findBreadcrumbPath(folderUuid, userId);

        return results.stream()
                .map(row -> new FileResponses.BreadcrumbItem(
                        (UUID) row[0],
                        (String) row[1]
                ))
                .toList();
    }
}
