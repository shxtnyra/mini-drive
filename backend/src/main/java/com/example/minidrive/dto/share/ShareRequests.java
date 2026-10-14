package com.example.minidrive.dto.share;

import com.example.minidrive.enums.SharePermission;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public class ShareRequests {
    public record Update(
            @NotNull UUID fileUuid,
            @Size(max = 50) List<@NotNull UUID> userUuids,
            @Size(max = 50) List<@NotNull UUID> groupUuids,
            SharePermission permission,
            Boolean linkEnabled
    ) {
        public Update {
            userUuids = userUuids == null ? List.of() : userUuids;
            groupUuids = groupUuids == null ? List.of() : groupUuids;
        }
    }
}
