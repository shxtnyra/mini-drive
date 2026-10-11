package com.example.minidrive.security;

import com.example.minidrive.enums.Role;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = parseBearerToken(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtProvider.getAccessClaims(token);

            Long id = Long.valueOf(claims.getSubject());
            UUID userUuid = UUID.fromString(claims.get("uuid", String.class));
            String username = claims.get("username", String.class);
            String email = claims.get("email", String.class);
            Boolean isAdmin = claims.get("is_admin", Boolean.class);
            Boolean emailVerified = claims.get("email_verified", Boolean.class);

            List<SimpleGrantedAuthority> authorities = Boolean.TRUE.equals(isAdmin)
                    ? List.of(Role.ROLE_USER.toAuthority(), Role.ROLE_ADMIN.toAuthority())
                    : List.of(Role.ROLE_USER.toAuthority());

            JwtUserDetails principal = new JwtUserDetails(
                    id,
                    userUuid, username, email,
                    Boolean.TRUE.equals(emailVerified),
                    Boolean.TRUE.equals(isAdmin),
                    authorities
            );

            var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        catch (Exception e) {
            log.debug("Не удалось аутентифицировать bearer-токен для {} {}",
                    request.getMethod(), request.getRequestURI(), e);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String parseBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer "))
            return null;

        return header.substring(7);
    }
}
