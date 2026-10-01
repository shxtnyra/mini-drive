package com.example.minidrive.dto.group;

import com.example.minidrive.entity.Group;
import java.time.Instant;

public class GroupResponses {
    public record Response(
            Long id,
            String name,
            int membersCount,
            long quotaBytes,
            Instant createdAt
    ) {
        public static Response fromEntity(Group group) {
            return new Response(
                    group.getId(),
                    group.getName(),
                    group.getMembersCount(),
                    group.getQuotaBytes(),
                    group.getCreatedAt()
            );
        }
    }
}