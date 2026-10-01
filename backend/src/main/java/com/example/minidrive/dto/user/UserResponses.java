package com.example.minidrive.dto.user;

import com.example.minidrive.dto.group.GroupResponses;
import com.example.minidrive.entity.Group;
import com.example.minidrive.entity.User;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class UserResponses {
    public record Summary(
            UUID uuid,
            String username,
            String email,
            String displayName
    ) {
        public static Summary fromEntity(User user) {
            return new Summary(
                    user.getUuid(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getDisplayName());
        }
    }

    public record Detail(
            UUID uuid,
            String username,
            String email,
            boolean emailVerified,
            String displayName,
            Long quotaBytes,
            List<GroupResponses.Response> groups,
            long usedBytes,
            boolean isAdmin,
            Instant createdAt,
            Instant deletedAt
    ) {
        public static Detail fromEntity(User user, long quotaBytes, List<Group> groups) {
            return new Detail(
                    user.getUuid(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getEmailVerifiedAt() != null,
                    user.getDisplayName(),
                    quotaBytes,
                    groups.stream().map(GroupResponses.Response::fromEntity).toList(),
                    user.getUsedBytes(),
                    user.getIsAdmin(),
                    user.getCreatedAt(),
                    user.getDeletedAt()
            );
        }
    }
}
