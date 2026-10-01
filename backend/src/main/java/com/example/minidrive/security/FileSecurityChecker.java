package com.example.minidrive.security;

import com.example.minidrive.repository.NodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.UUID;

@Component("fileSecurity")
@RequiredArgsConstructor
public class FileSecurityChecker {
    private final NodeRepository nodeRepository;

    // 1. Базовая проверка: может ли пользователь хотя бы ВИДЕТЬ этот узел?
    public boolean hasAccess(UUID nodeUuid, JwtUserDetails jwtUser) {
        if (nodeUuid == null) return true;
        if (jwtUser.isAdmin()) return true;

        String level = nodeRepository.getUserAccessLevel(nodeUuid, jwtUser.getId());
        return level != null && !level.equals("NONE"); // Или level != null, если запрос вернет null при отсутствии прав
    }

    // 2. Проверка на модификацию (переименование, перемещение)
    public boolean canModify(UUID nodeUuid, JwtUserDetails jwtUser) {
        if (nodeUuid == null) return false; // Нельзя модифицировать корень
        if (jwtUser.isAdmin()) return true;

        String level = nodeRepository.getUserAccessLevel(nodeUuid, jwtUser.getId());
        return "OWNER".equals(level) || "ALL".equals(level) || "WRITE".equals(level);
    }

    // 3. Проверка на удаление (только владелец или ALL)
    // Примечание: Если файл shared, удаление обычно означает "убрать из своего списка",
    // а не физическое удаление. Это бизнес-логика, но для безопасности требуем OWNER или ALL.
    public boolean canDelete(UUID nodeUuid, JwtUserDetails jwtUser) {
        if (nodeUuid == null) return false;
        if (jwtUser.isAdmin()) return true;

        String level = nodeRepository.getUserAccessLevel(nodeUuid, jwtUser.getId());
        return "OWNER".equals(level) || "ALL".equals(level);
    }

    // 4. Массовая проверка
    public boolean hasAccessToAll(List<UUID> nodeUuids, JwtUserDetails jwtUser) {
        if (nodeUuids == null || nodeUuids.isEmpty()) return true;
        if (jwtUser.isAdmin()) return true;

        List<UUID> accessibleUuids = nodeRepository.findAccessibleNodeUuids(nodeUuids, jwtUser.getId());
        return accessibleUuids.size() == nodeUuids.size();
    }
}