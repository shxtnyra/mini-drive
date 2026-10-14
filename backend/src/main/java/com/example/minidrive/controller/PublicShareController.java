package com.example.minidrive.controller;

import com.example.minidrive.dto.file.FileResponses;
import com.example.minidrive.dto.share.ShareResponses;
import com.example.minidrive.service.ShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/shares")
@RequiredArgsConstructor
public class PublicShareController {
    private final ShareService shareService;

    @GetMapping("/{token}")
    public ResponseEntity<ShareResponses.PublicNode> getSharedRoot(@PathVariable String token) {
        return ResponseEntity.ok(shareService.getPublicFile(token, null));
    }

    @GetMapping("/{token}/{filesUuid}")
    public ResponseEntity<ShareResponses.PublicNode> getSharedFile(
            @PathVariable String token,
            @PathVariable UUID filesUuid
    ) {
        return ResponseEntity.ok(shareService.getPublicFile(token, filesUuid));
    }

    @GetMapping("/{token}/{fileUuid}/download")
    public ResponseEntity<StreamingResponseBody> downloadSharedFile(
            @PathVariable String token,
            @PathVariable UUID fileUuid
    ) {
        FileResponses.DownloadResult file = shareService.downloadPublicFile(token, fileUuid);
        StreamingResponseBody stream = outputStream -> {
            try (InputStream inputStream = file.inputStream()) {
                inputStream.transferTo(outputStream);
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.mimeType()))
                .contentLength(file.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(file.fileName(), StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(stream);
    }
}
