package com.example.minidrive.controller;

import com.example.minidrive.dto.file.FileRequests;
import com.example.minidrive.dto.file.FileResponses;
import com.example.minidrive.security.JwtUserDetails;
import com.example.minidrive.service.FileUploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/files/upload")
@RequiredArgsConstructor
public class FileUploadController {
    public final FileUploadService fileUploadService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileResponses.Response> uploadFile(
            @Valid @RequestPart("metadata") FileRequests.DirectUpload request,
            @RequestPart("file") MultipartFile multipartFile,
            @AuthenticationPrincipal JwtUserDetails jwtUser
    ) {
        return ResponseEntity.ok(fileUploadService.uploadFile(request, multipartFile, jwtUser));
    }

    @PostMapping("/initiate")
    @PreAuthorize("@fileSecurity.canCreateInRoot(#request.parentUuid(), #jwtUser) or @fileSecurity.canModify(#request.parentUuid(), #jwtUser)")
    public ResponseEntity<FileResponses.BeginUpload> initiateUpload(
            @AuthenticationPrincipal JwtUserDetails jwtUser,
            @Valid @RequestBody FileRequests.BeginUpload request
            ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fileUploadService.initiateUpload(request, jwtUser));
    }

    @PostMapping("/{sessionUuid}/chunks")
    public ResponseEntity<Void> uploadChunk(
            @PathVariable UUID sessionUuid,
            @RequestParam("partNumber") int partNumber,
            @RequestParam("file") MultipartFile chunk,
            @AuthenticationPrincipal JwtUserDetails jwtUser) {

        fileUploadService.uploadChunk(sessionUuid, partNumber, chunk, jwtUser);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{sessionUuid}/status")
    public ResponseEntity<FileResponses.UploadStatus> getUploadStatus(
            @PathVariable UUID sessionUuid,
            @AuthenticationPrincipal JwtUserDetails jwtUser) {
        return ResponseEntity.ok(fileUploadService.getUploadStatus(sessionUuid, jwtUser));
    }

    @PostMapping("/{sessionUuid}:complete")
    public ResponseEntity<FileResponses.Response> completeUpload(
            @PathVariable UUID sessionUuid,
            @AuthenticationPrincipal JwtUserDetails jwtUser) {
        return ResponseEntity.ok(fileUploadService.completeUpload(sessionUuid, jwtUser));
    }

    @DeleteMapping("/{sessionUuid}:abort")
    public ResponseEntity<Void> abortUpload(
            @PathVariable UUID sessionUuid,
            @AuthenticationPrincipal JwtUserDetails jwtUser) {
        fileUploadService.abortUpload(sessionUuid, jwtUser);
        return ResponseEntity.noContent().build();
    }

}
