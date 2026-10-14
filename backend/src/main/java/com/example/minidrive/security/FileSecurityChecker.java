package com.example.minidrive.security;

import com.example.minidrive.repository.NodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.UUID;

/**
 * Эффективные уровни доступа:
 * 0 — нет доступа,
 * 1 — чтение,
 * 2 — запись,
 * 3 — полный доступ,
 * 4 — владелец.
 */

@Component("fileSecurity")
@RequiredArgsConstructor
public class FileSecurityChecker {
    private final NodeRepository nodeRepository;

    public boolean hasAccess(UUID nodeUuid, JwtUserDetails jwtUser) {
        // Корень не является Node; его содержимое фильтруется отдельно по владельцу.
        if (nodeUuid == null) return jwtUser != null;
        if (jwtUser.isAdmin()) return true;

        return hasRequiredLevel(List.of(nodeUuid), jwtUser, 1);
    }

    public boolean canModify(UUID nodeUuid, JwtUserDetails jwtUser) {
        if (jwtUser == null) return false;
        // Для корня используется отдельная проверка canCreateInRoot.
        if (nodeUuid == null) return true; // jwtUser != null
        if (jwtUser.isAdmin()) return true;

        return hasRequiredLevel(List.of(nodeUuid), jwtUser, 2);

    }

    public boolean canManageShares(UUID nodeUuid, JwtUserDetails jwtUser) {
        return jwtUser != null
                && nodeUuid != null
                && nodeRepository.existsByUuidAndOwner_IdAndDeletedAtIsNull(nodeUuid, jwtUser.getId());
    }

    // Корень диска не представлен в БД отдельным узлом, поэтому проверяется отдельно.
    public boolean canCreateInRoot(UUID parentUuid, JwtUserDetails jwtUser) {
        return parentUuid == null && jwtUser != null;
    }

    public boolean canReadAll(List<UUID> nodeUuids, JwtUserDetails jwtUser) {
        return hasRequiredLevel(nodeUuids, jwtUser, 1);
    }

    public boolean canModifyAll(List<UUID> nodeUuids, JwtUserDetails jwtUser) {
        return hasRequiredLevel(nodeUuids, jwtUser, 2);
    }

    public boolean canDeleteAll(List<UUID> nodeUuids, JwtUserDetails jwtUser) {
        return hasRequiredLevel(nodeUuids, jwtUser, 3);
    }

    private boolean hasRequiredLevel(List<UUID> nodeUuids, JwtUserDetails jwtUser, int requiredLevel) {
        if (jwtUser == null || nodeUuids == null || nodeUuids.isEmpty()) {
            return false;
        }

        if (nodeUuids.stream().distinct().count() != nodeUuids.size()) {
            return false;
        }

        if (jwtUser.isAdmin()) return true;

        List<UUID> allowedUuids = nodeRepository.findUuidsWithAccessLevel(
                nodeUuids,
                jwtUser.getId(),
                requiredLevel
        );
        return allowedUuids.size() == nodeUuids.size();
    }
}