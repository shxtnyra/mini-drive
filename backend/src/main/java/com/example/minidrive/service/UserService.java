package com.example.minidrive.service;

import com.example.minidrive.dto.common.CursorResponse;
import com.example.minidrive.dto.common.PageResponse;
import com.example.minidrive.dto.user.UserRequests;
import com.example.minidrive.dto.user.UserResponses;
import com.example.minidrive.entity.Group;
import com.example.minidrive.entity.User;
import com.example.minidrive.enums.ErrorCode;
import com.example.minidrive.exception.ApiException;
import com.example.minidrive.repository.GroupRepository;
import com.example.minidrive.repository.RefreshTokenRepository;
import com.example.minidrive.repository.UserRepository;
import com.example.minidrive.security.JwtUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final QuotaService quotaService;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public UserResponses.Detail getCurrentUser(JwtUserDetails jwtUser) {
        return toDetailDto(findUserOrThrow(jwtUser.getUuid()));
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public UserResponses.Detail updateProfile(JwtUserDetails jwtUser, UserRequests.UpdateProfile request) {
        User user = findUserOrThrow(jwtUser.getUuid());

        if (request.displayName() != null) {
            user.setDisplayName(request.displayName());
        }

        return toDetailDto(user);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN')")
    public PageResponse<UserResponses.Summary> getAllUsers(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);
        return PageResponse.of(users.map(UserResponses.Summary::fromEntity));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN')")
    public UserResponses.Detail setUserQuota(UUID userUuid, UserRequests.UpdateQuota request) {
        User user = findUserOrThrow(userUuid);
        user.setQuotaBytes(request.newQuotaBytes());
        quotaService.invalidateUserQuota(user.getId());
        return toDetailDto(user);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN')")
    public UserResponses.Detail setUserPassword(UUID userUuid, UserRequests.UpdatePassword request) {
        User user = findUserOrThrow(userUuid);
        Instant now = clock.instant();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        refreshTokenRepository.revokeAllByUserId(user.getId(), now);
        return toDetailDto(user);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN')")
    public UserResponses.Detail setAdminRole(UUID userUuid, boolean isAdmin) {
        User user = findUserOrThrow(userUuid);
        user.setIsAdmin(isAdmin);
        return toDetailDto(user);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN')")
    public void deleteUser(UUID userUuid) {
        User user = findUserOrThrow(userUuid);
        user.setDeletedAt(clock.instant());
        Instant now = clock.instant();
        refreshTokenRepository.revokeAllByUserId(user.getId(), now);
        quotaService.invalidateUserQuota(user.getId());
    }

    private User findUserOrThrow(UUID userUuid) {
        return userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }

    private UserResponses.Detail toDetailDto(User user) {
        long effectiveQuota = quotaService.getUserQuota(user.getId());
        List<Group> groups = groupRepository.getUserGroups(user.getId());
        return UserResponses.Detail.fromEntity(user, effectiveQuota, groups);
    }

    public CursorResponse<UserResponses.Summary, UUID> getAllUsers(Long cursor, int size) {
        List<UserResponses.Summary> users = userRepository.findNextPage(cursor, size + 1)
                .stream().map(UserResponses.Summary::fromEntity).toList();
        return CursorResponse.of(users, size, UserResponses.Summary::uuid);
    }
}