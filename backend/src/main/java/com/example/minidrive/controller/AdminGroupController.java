package com.example.minidrive.controller;

import com.example.minidrive.dto.common.MessageResponse;
import com.example.minidrive.dto.group.GroupRequests;
import com.example.minidrive.dto.group.GroupResponses;
import com.example.minidrive.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/groups")
@RequiredArgsConstructor
public class AdminGroupController {
    private final GroupService groupService;

    @PostMapping
    public ResponseEntity<GroupResponses.Response> createGroup(@Valid @RequestBody GroupRequests.Create request) {
        return ResponseEntity.ok(groupService.createGroup(request));
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<GroupResponses.Response> updateGroup(
            @PathVariable UUID groupUuid,
            @RequestBody GroupRequests.Update request
    ) {
        return ResponseEntity.ok(groupService.updateGroup(groupUuid, request));
    }

    @PostMapping("/{uuid}:set-quota")
    public ResponseEntity<GroupResponses.Response> setGroupQuota(
            @PathVariable UUID groupUuid,
            @RequestBody GroupRequests.SetQuota request
            ) {
        return ResponseEntity.ok(groupService.setGroupQuota(groupUuid, request));
    }

    @PostMapping("/{uuid}:add-members")
    public ResponseEntity<MessageResponse> addUsersToGroup(
            @PathVariable UUID groupUuid,
            @RequestBody List<UUID> usersUuid
    ) {
        groupService.addUsersToGroup(groupUuid, usersUuid);
        return ResponseEntity.ok(new MessageResponse("Пользователи добавлены в группу"));
    }

    @PostMapping("/{uuid}:remove-members")
    public ResponseEntity<MessageResponse> removeUsersFromGroup(
            @PathVariable UUID groupUuid,
            @RequestBody List<UUID> usersUuid
    ) {
        groupService.removeUsersFromGroup(groupUuid, usersUuid);
        return ResponseEntity.ok(new MessageResponse("Пользователи удалены из группы"));
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<MessageResponse> deleteGroup(@PathVariable UUID groupUuid) {
        groupService.deleteGroup(groupUuid);
        return ResponseEntity.ok(new MessageResponse("Группа удалена"));
    }
}
