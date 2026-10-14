package com.example.minidrive.controller;

import com.example.minidrive.dto.common.MessageResponse;
import com.example.minidrive.dto.file.FileRequests;
import com.example.minidrive.dto.file.FileResponses;
import com.example.minidrive.enums.Context;
import com.example.minidrive.security.JwtUserDetails;
import com.example.minidrive.service.FileService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {
    private final FileService fileService;

    @GetMapping
    public ResponseEntity<FileResponses.FolderResponse> getListFiles(
            @RequestParam(required = false, defaultValue = "DRIVE") Context context,
            @RequestParam(required = false) UUID parentUuid,
            @RequestParam(required = false) String nodeType,
            @RequestParam(required = false) List<String> mimeTypes,
            @RequestParam(required = false) UUID createdBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") String sortDirection,
            @RequestParam(required = false) String cursorValue,
            @RequestParam(required = false) UUID cursorUuid,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal JwtUserDetails jwtUser
    ) {
        return ResponseEntity.ok(fileService.getFilesByContext(context, parentUuid, nodeType,
                mimeTypes, createdBy, fromDate, toDate, sortBy, sortDirection, cursorValue, cursorUuid, size, jwtUser));
    }

    @PostMapping("/folders")
    public ResponseEntity<FileResponses.Response> createFolder(
            @Valid @RequestBody FileRequests.CreateFolder request,
            @AuthenticationPrincipal JwtUserDetails jwtUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fileService.createFolder(request, jwtUser));
    }

    @PatchMapping("/{fileUuid}")
    public ResponseEntity<FileResponses.Response> updateFile(
            @PathVariable UUID fileUuid,
            @Valid @RequestBody FileRequests.UpdateFile request,
            @AuthenticationPrincipal JwtUserDetails jwtUser
    ) {
        return ResponseEntity.ok(fileService.updateFile(fileUuid, request, jwtUser));
    }

    @PostMapping("/add-to-favorites")
    public ResponseEntity<MessageResponse> addToFavorites(
            @Valid @RequestBody FileRequests.BatchNodeRequest request,
            @AuthenticationPrincipal JwtUserDetails jwtUser
    ) {
        fileService.addToFavorites(request, jwtUser);
        return ResponseEntity.ok(new MessageResponse("Файл добавлен в избранное"));
    }

    @PostMapping("/move")
    public ResponseEntity<MessageResponse> moveFiles(
            @Valid @RequestBody FileRequests.MoveFiles request,
            @AuthenticationPrincipal JwtUserDetails jwtUser
    ) {
        fileService.moveFiles(request, jwtUser);
        return ResponseEntity.ok(new MessageResponse("Элементы успешно перемещены"));
    }

    @PostMapping("/remove-from-favorites")
    public ResponseEntity<MessageResponse> removeFromFavorites(
            @Valid @RequestBody FileRequests.BatchNodeRequest request,
            @AuthenticationPrincipal JwtUserDetails jwtUser
    ) {
        fileService.removeFromFavorites(request, jwtUser);
        return ResponseEntity.ok(new MessageResponse("Файл добавлен в избранное"));
    }

    @PostMapping("/restore")
    public ResponseEntity<MessageResponse> restoreFile(
            @Valid @RequestBody FileRequests.BatchNodeRequest request,
            @AuthenticationPrincipal JwtUserDetails jwtUser
    ) {
        fileService.restoreFile(request, jwtUser);
        return ResponseEntity.ok(new MessageResponse("Элемент успешно восстановлен"));
    }

    @GetMapping("/{fileUuid}/download")
    public ResponseEntity<StreamingResponseBody> downloadFile(
            @PathVariable UUID fileUuid,
            @AuthenticationPrincipal JwtUserDetails jwtUser
    ) {
        FileResponses.DownloadResult result = fileService.downloadFile(fileUuid, jwtUser);

        StreamingResponseBody stream = outputStream -> {
            try (InputStream is = result.inputStream()) {
                is.transferTo(outputStream);
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.mimeType()))
                .contentLength(result.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + result.fileName() + "\"")
                .body(stream);
    }

    @PostMapping("/download-zip")
    public ResponseEntity<StreamingResponseBody> downloadFilesAsZip  (
            @Valid @RequestBody FileRequests.BatchNodeRequest request,
            @AuthenticationPrincipal JwtUserDetails jwtUser
    ) {
        fileService.validateZipAccess(request, jwtUser);
        Path archive = fileService.createZipArchive(request);
        long archiveSize;
        try {
            archiveSize = Files.size(archive);
        } catch (IOException exception) {
            deleteTemporaryArchive(archive);
            throw new IllegalStateException("Could not inspect completed ZIP archive", exception);
        }

        StreamingResponseBody stream = outputStream -> {
            try {
                Files.copy(archive, outputStream);
            } catch (IOException exception) {
                log.warn("ZIP download stream failed for user {}", jwtUser.getId(), exception);
                throw exception;
            } finally {
                deleteTemporaryArchive(archive);
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .contentLength(archiveSize)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"archive.zip\"")
                .body(stream);
    }

    @DeleteMapping()
    public ResponseEntity<MessageResponse> deleteFile(
            @Valid @RequestBody FileRequests.BatchNodeRequest request,
            @AuthenticationPrincipal JwtUserDetails jwtUser
    ) {
        fileService.deleteFile(request, jwtUser);
        return ResponseEntity.ok(new MessageResponse("Элемент успешно перемещен в корзину"));
    }

    private void deleteTemporaryArchive(Path archive) {
        try {
            Files.deleteIfExists(archive);
        } catch (IOException exception) {
            log.error("Не удалось удалить временный архив {}", archive, exception);
        }
    }
}