package com.example.minidrive.controller;

import com.example.minidrive.dto.auth.AuthResponses;
import com.example.minidrive.dto.common.MessageResponse;
import com.example.minidrive.dto.common.PageResponse;
import com.example.minidrive.dto.user.UserRequests;
import com.example.minidrive.dto.user.UserResponses;
import com.example.minidrive.service.AuthService;
import com.example.minidrive.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final UserService userService;
    private final AuthService authService;

    @GetMapping
    public ResponseEntity<PageResponse<UserResponses.Summary>> getAllUsers(
            @PageableDefault Pageable pageable
    ) {
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    @GetMapping("{uuid}/sessions")
    public ResponseEntity<List<AuthResponses.Session>> getUserSession(@PathVariable UUID uuid) {
        return ResponseEntity.ok(authService.getUserSessions(uuid));
    }

    @PostMapping("{uuid}/sessions:revoke-all")
    public ResponseEntity<MessageResponse> revokeAllUserSession(@PathVariable UUID uuid) {
        authService.revokeAllUserTokens(uuid);
        return ResponseEntity.ok(new MessageResponse("Все сессии пользователя завершены"));
    }

    @PostMapping("/{uuid}:set-quota")
    public ResponseEntity<UserResponses.Detail> updateUserQuota(
            @PathVariable UUID uuid,
            @Valid @RequestBody UserRequests.UpdateQuota request
    ) {
        return ResponseEntity.ok(userService.setUserQuota(uuid, request));
    }

    @PostMapping("/{uuid}:change-role")
    public ResponseEntity<UserResponses.Detail> setAdminRole(
            @PathVariable UUID uuid,
            @RequestParam boolean isAdmin
    ) {
        return ResponseEntity.ok(userService.setAdminRole(uuid, isAdmin));
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<MessageResponse> deleteUser(@PathVariable UUID uuid) {
        userService.deleteUser(uuid);
        return ResponseEntity.ok(new MessageResponse("Пользователь успешно заблокирован"));
    }
}
