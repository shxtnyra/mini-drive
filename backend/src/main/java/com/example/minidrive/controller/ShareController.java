package com.example.minidrive.controller;

import com.example.minidrive.dto.share.ShareRequests;
import com.example.minidrive.dto.share.ShareResponses;
import com.example.minidrive.security.JwtUserDetails;
import com.example.minidrive.service.ShareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shares")
@RequiredArgsConstructor
public class ShareController {
    private final ShareService shareService;

    @GetMapping("/{fileUuid}")
    public ResponseEntity<ShareResponses.Details> getShares(
            @PathVariable UUID fileUuid,
            @AuthenticationPrincipal JwtUserDetails jwtUser
    ) {
        return ResponseEntity.ok(shareService.getShares(fileUuid, jwtUser));
    }

    @PostMapping
    public ResponseEntity<ShareResponses.Details> updateShares(
            @Valid @RequestBody ShareRequests.Update request,
            @AuthenticationPrincipal JwtUserDetails jwtUser
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(shareService.updateShares(request, jwtUser));
    }

    @DeleteMapping("/{shareUuid}")
    public ResponseEntity<Void> revokeShare(
            @PathVariable UUID shareUuid,
            @AuthenticationPrincipal JwtUserDetails jwtUser
    ) {
        shareService.revokeShare(shareUuid, jwtUser);
        return ResponseEntity.noContent().build();
    }
}
