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

    @PutMapping("/{groupId}")
    public ResponseEntity<GroupResponses.Response> updateGroup(
            @PathVariable Long groupId,
            @RequestBody GroupRequests.Update request
    ) {
        return ResponseEntity.ok(groupService.updateGroup(groupId, request));
    }

    @PostMapping("/{groupId}:set-quota")
    public ResponseEntity<GroupResponses.Response> setGroupQuota(
            @PathVariable Long groupId,
            @RequestBody GroupRequests.SetQuota request
            ) {
        return ResponseEntity.ok(groupService.setGroupQuota(groupId, request));
    }

    @PostMapping("/{groupId}:add-members")
    public ResponseEntity<MessageResponse> addUsersToGroup(
            @PathVariable Long groupId,
            @RequestBody List<UUID> usersUuid
    ) {
        groupService.addUsersToGroup(groupId, usersUuid);
        return ResponseEntity.ok(new MessageResponse("Пользователи добавлены в группу"));
    }

    @PostMapping("/{groupId}:remove-members")
    public ResponseEntity<MessageResponse> removeUsersFromGroup(
            @PathVariable Long groupId,
            @RequestBody List<UUID> usersUuid
    ) {
        groupService.removeUsersFromGroup(groupId, usersUuid);
        return ResponseEntity.ok(new MessageResponse("Пользователи удалены из группы"));
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<MessageResponse> deleteGroup(@PathVariable Long groupId) {
        groupService.deleteGroup(groupId);
        return ResponseEntity.ok(new MessageResponse("Группа удалена"));
    }
}
