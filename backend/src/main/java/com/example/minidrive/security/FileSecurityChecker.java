package com.example.minidrive.security;

import com.example.minidrive.repository.NodeRepository;
import com.example.minidrive.dto.file.FileRequests;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.UUID;

/**
 * Эффективные уровни доступа: 0 — нет доступа, 1 — чтение, 2 — запись,
 * 3 — полный доступ по share, 4 — владелец.
 */

@Component("fileSecurity")
@RequiredArgsConstructor
public class FileSecurityChecker {
    private final NodeRepository nodeRepository;

    public boolean hasAccess(UUID nodeUuid, JwtUserDetails jwtUser) {
        // Корень не является Node; его содержимое фильтруется отдельно по владельцу.
        if (nodeUuid == null) return jwtUser != null;
        if (jwtUser.isAdmin()) return true;

        Integer level = nodeRepository.getUserAccessLevel(nodeUuid, jwtUser.getId());
        return level != null && level >= 1;
    }

    public boolean canModify(UUID nodeUuid, JwtUserDetails jwtUser) {
        if (jwtUser == null) return false;
        // Для корня используется отдельная проверка canCreateInRoot.
        if (nodeUuid == null) return false;
        if (jwtUser.isAdmin()) return true;

        Integer level = nodeRepository.getUserAccessLevel(nodeUuid, jwtUser.getId());
        return level != null && level >= 2;
    }

    public boolean canDelete(UUID nodeUuid, JwtUserDetails jwtUser) {
        if (jwtUser == null || nodeUuid == null) return false;
        if (jwtUser.isAdmin()) return true;

        Integer level = nodeRepository.getUserAccessLevel(nodeUuid, jwtUser.getId());
        // Уровня WRITE недостаточно для удаления: нужны ALL или владение узлом.
        return level != null && level >= 3;
    }

    /**
     * Корень диска не представлен в БД отдельным узлом, поэтому проверяется отдельно.
     */
    public boolean canCreateInRoot(UUID parentUuid, JwtUserDetails jwtUser) {
        return parentUuid == null && jwtUser != null;
    }

    public boolean canModifyAll(List<UUID> nodeUuids, JwtUserDetails jwtUser) {
        return hasRequiredLevel(nodeUuids, jwtUser, 2);
    }

    public boolean canDeleteAll(List<UUID> nodeUuids, JwtUserDetails jwtUser) {
        return hasRequiredLevel(nodeUuids, jwtUser, 3);
    }

    public boolean canReadAll(FileRequests.BatchNodeRequest request, JwtUserDetails jwtUser) {
        return request != null && hasRequiredLevel(request.fileUuids(), jwtUser, 1);
    }

    public boolean canDeleteAll(FileRequests.BatchNodeRequest request, JwtUserDetails jwtUser) {
        return request != null && hasRequiredLevel(request.fileUuids(), jwtUser, 3);
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