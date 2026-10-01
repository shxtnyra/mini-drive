package com.example.minidrive.controller;

import com.example.minidrive.dto.auth.AuthRequests;
import com.example.minidrive.dto.auth.AuthResponses;
import com.example.minidrive.dto.common.ClientInfo;
import com.example.minidrive.dto.common.MessageResponse;
import com.example.minidrive.dto.user.UserRequests;
import com.example.minidrive.dto.user.UserResponses;
import com.example.minidrive.security.JwtUserDetails;
import com.example.minidrive.service.AuthService;
import com.example.minidrive.service.UserService;
import com.example.minidrive.util.ClientInfoExtractor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final AuthService authService;
    private final ClientInfoExtractor clientInfoExtractor;

//    @GetMapping
//    public ResponseEntity<CursorResponse<UserResponses.Summary, UUID>> getUsers(
//            @RequestParam(required = false) Long cursor,
//            @RequestParam(defaultValue = "20") int size
//    ) {
//        return ResponseEntity.ok(userService.getAllUsers(cursor, size));
//    }

    @GetMapping("/me")
    public ResponseEntity<UserResponses.Detail> getCurrentUser(@AuthenticationPrincipal JwtUserDetails jwtUser) {
        return ResponseEntity.ok(userService.getCurrentUser(jwtUser));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponses.Detail> updateProfile(
            @AuthenticationPrincipal JwtUserDetails jwtUser,
            @RequestBody UserRequests.UpdateProfile request
            ) {
        return ResponseEntity.ok(userService.updateProfile(jwtUser, request));
    }

    @PutMapping("/me/password")
    public ResponseEntity<MessageResponse> changePassword(
            @Valid @RequestBody AuthRequests.ChangePassword request,
            @AuthenticationPrincipal JwtUserDetails jwtUser
    ) {
        authService.changePassword(jwtUser, request);
        return ResponseEntity.ok(new MessageResponse("Пароль успешно изменен. Вам потребуется войти заново."));
    }

    @PostMapping("/me/sessions:revoke:all")
    public ResponseEntity<MessageResponse> revokeAllSessions(@AuthenticationPrincipal JwtUserDetails jwtUser) {
        authService.revokeAllUserTokens(jwtUser.getUuid());
        return ResponseEntity.ok(new MessageResponse("Все сессии завершены"));
    }

    @GetMapping("/me/sessions")
    public ResponseEntity<List<AuthResponses.Session>> getSessions(
            @AuthenticationPrincipal JwtUserDetails jwtUser) {

        return ResponseEntity.ok(authService.getUserSessions(jwtUser.getUuid()));
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserResponses.Summary>> searchUser(
            @RequestParam("query") String query,
            @AuthenticationPrincipal JwtUserDetails jwtUser
    ) {
        return ResponseEntity.ok(userService.searchUsersByNameOrEmail(query));
    }

    @PostMapping("me/email:verify")
    public ResponseEntity<AuthResponses.Tokens> verifyEmail(
            @Valid @RequestBody AuthRequests.VerifyEmail request,
            @AuthenticationPrincipal JwtUserDetails jwtUser,
            HttpServletRequest httpRequest
    ) {
        ClientInfo clientInfo = clientInfoExtractor.extractClientInfo(httpRequest);
        return ResponseEntity.ok(authService.verifyEmail(request, jwtUser, clientInfo));
    }

    @PostMapping("me/email:resend-code")
    public ResponseEntity<MessageResponse> resendEmailCode(
            @AuthenticationPrincipal JwtUserDetails jwtUser
    ) {
        authService.resendEmailVerificationCode(jwtUser);
        return ResponseEntity.ok(new MessageResponse("Новый код отправлен на email"));
    }

    @PostMapping("/email/change")
    public ResponseEntity<MessageResponse> requestEmailChange(
            @Valid @RequestBody AuthRequests.EmailChange request,
            @AuthenticationPrincipal JwtUserDetails jwtUser
    ) {
        authService.requestEmailChange(request, jwtUser);
        return ResponseEntity.ok(new MessageResponse("Код подтверждения отправлен на новый email"));
    }

    @PostMapping("/email/change:verify")
    public ResponseEntity<AuthResponses.Tokens> confirmEmailChange(
            @Valid @RequestBody AuthRequests.VerifyEmail request,
            @AuthenticationPrincipal JwtUserDetails jwtUser,
            HttpServletRequest httpRequest
    ) {
        ClientInfo clientInfo = clientInfoExtractor.extractClientInfo(httpRequest);
        return ResponseEntity.ok(authService.confirmEmailChange(request, jwtUser, clientInfo));
    }
}
