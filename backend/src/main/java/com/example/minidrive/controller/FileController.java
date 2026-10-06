package com.example.minidrive.controller;

import com.example.minidrive.dto.common.MessageResponse;
import com.example.minidrive.dto.file.FileRequests;
import com.example.minidrive.dto.file.FileResponses;
import com.example.minidrive.security.JwtUserDetails;
import com.example.minidrive.service.FileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;
import java.util.zip.Deflater;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("api/v1/files")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;

    @GetMapping
    public ResponseEntity<FileResponses.FolderResponse> getListFiles(
            @RequestParam(required = false, defaultValue = "DRIVE") String context,
            @RequestParam(required = false) UUID parentUuid,
            @RequestParam(required = false) String nodeType,
            @RequestParam(required = false) String mimeType,
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
                mimeType, createdBy, fromDate, toDate, sortBy, sortDirection, cursorValue, cursorUuid, size, jwtUser));
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
            @RequestBody FileRequests.BatchNodeRequest request,
            @AuthenticationPrincipal JwtUserDetails jwtUser
    ) {
        fileService.addToFavorites(request, jwtUser);
        return ResponseEntity.ok(new MessageResponse("Файл добавлен в избранное"));
    }

    @PostMapping("/remove-from-favorites")
    public ResponseEntity<MessageResponse> removeFromFavorites(
            @RequestBody FileRequests.BatchNodeRequest request,
            @AuthenticationPrincipal JwtUserDetails jwtUser
    ) {
        fileService.removeFromFavorites(request, jwtUser);
        return ResponseEntity.ok(new MessageResponse("Файл добавлен в избранное"));
    }

    @DeleteMapping()
    public ResponseEntity<MessageResponse> deleteFile(
            @RequestBody FileRequests.BatchNodeRequest request,
            @AuthenticationPrincipal JwtUserDetails jwtUser
    ) {
        fileService.deleteFile(request, jwtUser);
        return ResponseEntity.ok(new MessageResponse("Элемент успешно перемещен в корзину"));
    }

    @PostMapping("/restore")
    public ResponseEntity<MessageResponse> restoreFile(
            @RequestBody FileRequests.BatchNodeRequest request,
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
            @RequestBody FileRequests.BatchNodeRequest request,
            @AuthenticationPrincipal JwtUserDetails jwtUser
    ) {
        fileService.validateZipAccess(request, jwtUser);

        StreamingResponseBody stream = outputStream -> {
            try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
                zipOut.setLevel(Deflater.NO_COMPRESSION);
                fileService.streamZipFiles(request, zipOut);
            } catch (Exception e) {
                System.err.println("КРИТИЧЕСКАЯ ОШИБКА при записи ZIP: " + e.getClass().getName() + " - " + e.getMessage());
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"archive.zip\"")
                .body(stream);
    }
}