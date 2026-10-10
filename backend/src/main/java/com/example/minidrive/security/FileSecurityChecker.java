package com.example.minidrive.security;

import com.example.minidrive.repository.NodeRepository;
import com.example.minidrive.dto.file.FileRequests;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.UUID;

/*
Про права:
    0 — нет доступа
    1 — только чтение
    2 — чтение и запись
    3 — полный доступ (ALL)
    4 — владелец
 */

@Component("fileSecurity")
@RequiredArgsConstructor
public class FileSecurityChecker {
    private final NodeRepository nodeRepository;

    // Базовая проверка: может ли пользователь хотя бы УЗРЕТЬ этот узел?
    public boolean hasAccess(UUID nodeUuid, JwtUserDetails jwtUser) {
        // Отсутствие parentUuid означает корень диска, а его содержимое
        // дополнительно ограничивается владельцем в SQL выборки списка.
        if (nodeUuid == null) return jwtUser != null;
        if (jwtUser.isAdmin()) return true;

        Integer level = nodeRepository.getUserAccessLevel(nodeUuid, jwtUser.getId());
        return level != null && level >= 1;
    }

    // 2. Проверка на модификацию
    public boolean canModify(UUID nodeUuid, JwtUserDetails jwtUser) {
        if (jwtUser == null) return false;
        // Корень не является Node. Для него используется отдельная проверка ниже из-за того что
        // я не могу использовать isAuthenticated в связвке с or.
        if (nodeUuid == null) return false;
        if (jwtUser.isAdmin()) return true;

        Integer level = nodeRepository.getUserAccessLevel(nodeUuid, jwtUser.getId());
        return level != null && level >= 2;
    }

    // 3. Проверка на удаление
    public boolean canDelete(UUID nodeUuid, JwtUserDetails jwtUser) {
        if (jwtUser == null || nodeUuid == null) return false;
        if (jwtUser.isAdmin()) return true;

        Integer level = nodeRepository.getUserAccessLevel(nodeUuid, jwtUser.getId());
        // WRITE может разрешать изменение содержимого, но удаление подвласно
        // только владельцу или пользователю с полным правом ALL.
        return level != null && level >= 3;
    }

    /**
     * Создание узла в корне разрешено аутентифицированному пользователю:
     */
    public boolean canCreateInRoot(UUID parentUuid, JwtUserDetails jwtUser) {
        return parentUuid == null && jwtUser != null;
    }

    /**
     * Проверяет пакет UUID одним запросом и требует права записи на каждом узле.
     */
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