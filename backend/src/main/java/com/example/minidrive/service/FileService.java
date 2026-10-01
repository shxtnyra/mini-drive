package com.example.minidrive.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.example.minidrive.provider.StorageProvider;
import com.example.minidrive.provider.StorageProviderFactory;
import com.example.minidrive.repository.NodeJdbcRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.minidrive.dto.common.CursorResponse;
import com.example.minidrive.dto.file.FileRequests;
import com.example.minidrive.dto.file.FileResponses;
import com.example.minidrive.entity.Node;
import com.example.minidrive.entity.User;
import com.example.minidrive.enums.ErrorCode;
import com.example.minidrive.enums.NodeType;
import com.example.minidrive.exception.ApiException;
import com.example.minidrive.repository.NodeRepository;
import com.example.minidrive.repository.UserRepository;
import com.example.minidrive.security.JwtUserDetails;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileService {
    private final NodeRepository nodeRepository;
    private final NodeJdbcRepository nodeJdbcRepository;
    private final UserRepository userRepository;
    private final StorageProviderFactory storageProviderFactory;
    private final Clock clock;

//    @Transactional(readOnly = true)
//    @PreAuthorize("@fileSecurity.hasAccess(#parentUuid, #jwtUser)")
//    public FileResponses.FolderResponse getFiles(
//            UUID parentUuid,
//            UUID cursor,
//            int size,
//            JwtUserDetails jwtUser
//    ) {
//        Node parent = getParentFolder(parentUuid);
//        Long parentId = parent != null ? parent.getId() : null;
//        Long userId = jwtUser.getId();
//
//        List<FileResponses.Response> files = nodeRepository.findNextPage(userId, cursor, parentId, size + 1)
//                .stream()
//                .map(FileResponses.Response::fromEntity)
//                .toList();
//
//        List<FileResponses.BreadcrumbItem> breadcrumbs = buildBreadcrumbs(parentUuid, userId);
//
//        return new FileResponses.FolderResponse(breadcrumbs,
//                CursorResponse.of(files, size, FileResponses.Response::uuid));
//    }

    @Transactional(readOnly = true)
    @PreAuthorize("@fileSecurity.hasAccess(#parentUuid, #jwtUser)")
    public FileResponses.FolderResponse getFilesByContext(
            String context,
            UUID parentUuid,
            String nodeType,
            String mimeType,
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

        List<UUID> uuids = nodeJdbcRepository.findNodeUuidsWithFilters(
                context, jwtUser.getId(), parentId,
                nodeType, mimeType, createdById, fromDate, toDate,
                sortBy, sortDirection, parsedCursorValue, cursorId, size
        );

        List<FileResponses.BreadcrumbItem> breadcrumbs = buildBreadcrumbs(parentUuid, jwtUser.getId());

        if (uuids.isEmpty()) {
            return new FileResponses.FolderResponse(
                    breadcrumbs,
                    CursorResponse.of()
            );
        }

        Map<UUID, Node> nodeMap = nodeRepository.findAllByUuidIn(uuids).stream()
                .collect(Collectors.toMap(Node::getUuid, n -> n));

        List<FileResponses.Response> filesResponse = uuids.stream()
                .map(nodeMap::get)
                .filter(Objects::nonNull)
                .map(FileResponses.Response::fromEntity)
                .toList();

        CursorResponse<FileResponses.Response, CursorResponse.CursorDto> cursorPage = CursorResponse.of(
                filesResponse,
                size,
                file -> extractCursor(file, sortBy)
        );

        return new FileResponses.FolderResponse(breadcrumbs, cursorPage);
    }


    @Transactional(readOnly = true)
    @PreAuthorize("@fileSecurity.hasAccessToAll(#request.fileUuids(), #jwtUser)")
    public void validateZipAccess(FileRequests.BatchNodeRequest request, JwtUserDetails jwtUser) {
        if (request == null || request.fileUuids() == null || request.fileUuids().isEmpty()) {
            throw new ApiException(ErrorCode.FILE_NOT_FOUND);
        }

        // Аннотация @PreAuthorize автоматически проверит доступ ко всем request.fileUuids().
        // Если доступа нет — Spring сразу выбросит AccessDeniedException (403 Forbidden)
        // до того, как откроется асинхронный поток StreamingResponseBody.
    }

    @Transactional()
    @PreAuthorize("@fileSecurity.hasAccess(#parentUuid, #jwtUser)")
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

    @Transactional(readOnly = true)
    @PreAuthorize("@fileSecurity.hasAccess(#parentUuid, #jwtUser)")
    public CursorResponse<FileResponses.Response, UUID> getTrashFiles(
            UUID cursor,
            int size,
            JwtUserDetails jwtUser
    ) {
        Long userId = jwtUser.getId();

        List<FileResponses.Response> files = nodeRepository
                .findNextPageTrash(userId, cursor, size + 1)
                .stream()
                .map(FileResponses.Response::fromEntity)
                .toList();

        return CursorResponse.of(files, size, FileResponses.Response::uuid);
    }

    @Transactional
    @PreAuthorize("@fileSecurity.hasAccess(#request.parentUuid(), #jwtUser)")
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
    @PreAuthorize("@fileSecurity.hasAccessToAll(#request.fileUuids(), #jwtUser)")
    public void deleteFile(FileRequests.BatchNodeRequest request, JwtUserDetails jwtUser) {
        Instant now = clock.instant();
        // Првоерок на существоание нет так как они проверяются при проверке прав
        nodeRepository.markAsDeleted(request.fileUuids(), now);
    }

    @Transactional
    @PreAuthorize("@fileSecurity.hasAccessToAll(#request.fileUuids(), #jwtUser)")
    public void restoreFile(FileRequests.BatchNodeRequest request, JwtUserDetails jwtUser) {
        nodeRepository.unmarkAsDeleted(request.fileUuids());
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
                file.getMimeType() != null ? file.getMimeType() : "application/octet-stream",
                file.getSizeBytes()
        );
    }

    public void streamZipFiles(FileRequests.BatchNodeRequest request, ZipOutputStream zipOut) {
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

    @Transactional
    @PreAuthorize(value = "@fileSecurity.hasAccess(#fileUuid, #jwtUser)")
    public void makeFileFavorite(UUID fileUuid, JwtUserDetails jwtUser) {
        Node file = nodeRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new ApiException(ErrorCode.FILE_NOT_FOUND));

        nodeRepository.makeFileFavorite(file.getId(), jwtUser.getId());
    }

    @Transactional
    @PreAuthorize(value = "@fileSecurity.hasAccess(#fileUuid, #jwtUser)")
    public void makeFileUnfavorite(UUID fileUuid, JwtUserDetails jwtUser) {
        Node file = nodeRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new ApiException(ErrorCode.FILE_NOT_FOUND));

        nodeRepository.makeFileUnfavorite(file.getId(), jwtUser.getId());
    }

    private void compressNodeRecursive(Node file, String parentPath, ZipOutputStream zipOut,  Map<Long, List<Node>> childrenMap) {
        // Формируем правильный путь для текущего узла (файла или папки)
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
            default -> file.createdAt().toString(); // По умолчанию createdAt
        };

        return new CursorResponse.CursorDto(value, file.uuid());
    }

    // TODO: можно кешировать в redis
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
