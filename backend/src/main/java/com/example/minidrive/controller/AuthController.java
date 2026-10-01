    package com.example.minidrive.controller;

    import com.example.minidrive.dto.auth.AuthRequests;
    import com.example.minidrive.dto.auth.AuthResponses;
    import com.example.minidrive.dto.common.ClientInfo;
    import com.example.minidrive.dto.common.MessageResponse;
    import com.example.minidrive.service.AuthService;
    import com.example.minidrive.util.ClientInfoExtractor;
    import jakarta.servlet.http.HttpServletRequest;
    import jakarta.validation.Valid;
    import lombok.RequiredArgsConstructor;
    import org.springframework.http.ResponseEntity;
    import org.springframework.web.bind.annotation.*;

    @RestController
    @RequestMapping("/api/v1/auth")
    @RequiredArgsConstructor
    public class AuthController {
        private final AuthService authService;
        private final ClientInfoExtractor clientInfoExtractor;

        @PostMapping("/register")
        public ResponseEntity<AuthResponses.Tokens> register(@Valid @RequestBody AuthRequests.Register request,
                                                             HttpServletRequest httpRequest) {
            ClientInfo clientInfo = clientInfoExtractor.extractClientInfo(httpRequest);
            return ResponseEntity.status(201).body(authService.register(request, clientInfo));
        }

        @PostMapping("/login")
        public ResponseEntity<AuthResponses.Tokens> login(@Valid @RequestBody AuthRequests.Login request,
                                                   HttpServletRequest httpRequest) {
            ClientInfo clientInfo = clientInfoExtractor.extractClientInfo(httpRequest);
            return ResponseEntity.ok(authService.login(request, clientInfo));
        }

        @PostMapping("/refresh")
        public ResponseEntity<AuthResponses.Tokens> refresh(@Valid @RequestBody AuthRequests.Refresh request,
                                                     HttpServletRequest httpRequest) {
            ClientInfo clientInfo = clientInfoExtractor.extractClientInfo(httpRequest);
            return ResponseEntity.ok(authService.refreshTokens(request.refreshToken(), clientInfo));
        }

        @PostMapping("/logout")
        public ResponseEntity<MessageResponse> logout(@Valid @RequestBody AuthRequests.Refresh request,
                                                      HttpServletRequest httpRequest) {
            authService.revokeRefreshToken(request.refreshToken());
            return ResponseEntity.ok(new MessageResponse("Успешный выход"));
        }

        @PostMapping("/reset/password-resets")
        public ResponseEntity<MessageResponse> requestResetPassword(
                @Valid @RequestBody AuthRequests.PasswordReset request
        ) {
            authService.requestPasswordReset(request);
            return ResponseEntity.ok(new MessageResponse("Код отправлен на email"));
        }

        @PostMapping("/reset/password-resets:verify")
        public ResponseEntity<AuthResponses.Tokens> verifyResetPasswordCode(
                @Valid @RequestBody AuthRequests.VerifyPasswordReset request
        ) {
            return ResponseEntity.ok(authService.verifyPasswordResetCode(request));
        }

        @PostMapping("/reset/password-resets:reset")
        public ResponseEntity<MessageResponse> resetResetPassword(
                @Valid @RequestBody AuthRequests.ConfirmPasswordReset request
        ) {
            authService.confirmPasswordReset(request);
            return ResponseEntity.ok(new MessageResponse("Пароль успешно сброшен. Войдите с новым паролем."));
        }
    }
