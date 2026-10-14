package com.example.minidrive.controller;

import com.example.minidrive.dto.common.PageResponse;
import com.example.minidrive.dto.group.GroupResponses;
import com.example.minidrive.dto.user.UserResponses;
import com.example.minidrive.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController {
    private final GroupService groupService;

    @GetMapping
    public ResponseEntity<PageResponse<GroupResponses.Response>> getGroups(@PageableDefault() Pageable pageable) {
        return ResponseEntity.ok(groupService.getGroups(pageable));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<PageResponse<UserResponses.Summary>> getUsersFromGroup(@PathVariable UUID uuid, @PageableDefault Pageable pageable) {
        return ResponseEntity.ok(groupService.getUsersFromGroup(uuid, pageable));
    }
}
