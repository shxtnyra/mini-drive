package com.example.minidrive.dto.group;

import com.example.minidrive.entity.Group;
import java.time.Instant;
import java.util.UUID;

public class GroupResponses {
    public record Response(
            UUID uuid,
            String name,
            int membersCount,
            long quotaBytes,
            Instant createdAt
    ) {
        public static Response fromEntity(Group group) {
            return new Response(
                    group.getUuid(),
                    group.getName(),
                    group.getMembersCount(),
                    group.getQuotaBytes(),
                    group.getCreatedAt()
            );
        }
    }
}