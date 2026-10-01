package com.example.minidrive.service;

import com.example.minidrive.dto.common.PageResponse;
import com.example.minidrive.dto.group.GroupRequests;
import com.example.minidrive.dto.group.GroupResponses;
import com.example.minidrive.dto.user.UserResponses;
import com.example.minidrive.entity.Group;
import com.example.minidrive.entity.User;
import com.example.minidrive.enums.ErrorCode;
import com.example.minidrive.exception.ApiException;
import com.example.minidrive.repository.GroupRepository;
import com.example.minidrive.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupService {
    private final QuotaService quotaService;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN')")
    public PageResponse<GroupResponses.Response> getGroups(Pageable pageable) {
        Page<GroupResponses.Response> groups = groupRepository.findAll(pageable)
                .map(GroupResponses.Response::fromEntity);
        return PageResponse.of(groups);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN')")
    public GroupResponses.Response createGroup(GroupRequests.Create request) {
        if (groupRepository.existsByName(request.name())) {
            throw new ApiException(ErrorCode.GROUP_EXISTS);
        }

        Group group = Group.builder()
                .name(request.name())
                .build();

        groupRepository.save(group);
        return GroupResponses.Response.fromEntity(group);
    }

    public PageResponse<UserResponses.Summary> getUsersFromGroup(Long groupId, Pageable pageable) {
        Group group = findGroupOrThrow(groupId);

        Page<UserResponses.Summary> users = userRepository.findByGroup(group.getId(), pageable)
                .map(UserResponses.Summary::fromEntity);

        return PageResponse.of(users);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN')")
    public void addUsersToGroup(Long groupId, List<UUID> usersUuid) {
        Group group = findGroupOrThrow(groupId);

        List<User> users = userRepository.findAllByUuidIn(usersUuid);

        if (users.isEmpty()) {
            return;
        }

        List<Long> userIds = users.stream().map(User::getId).toList();
        groupRepository.addMembers(group.getId(), userIds);
        userIds.forEach(quotaService::invalidateUserQuota);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN')")
    public void removeUsersFromGroup(Long groupId, List<UUID> usersUuid) {
        if (!groupRepository.existsById(groupId))
                throw new ApiException(ErrorCode.GROUP_NOT_FOUND);

        List<User> users = userRepository.findAllByUuidIn(usersUuid);

        if (users.isEmpty()) {
            return;
        }

        List<Long> userIds = users.stream().map(User::getId).toList();
        groupRepository.removeMembers(groupId, userIds);
        userIds.forEach(quotaService::invalidateUserQuota);
    }

    @Transactional
    public GroupResponses.Response setGroupQuota(Long groupId, GroupRequests.SetQuota request) {
        Group group = findGroupOrThrow(groupId);
        group.setQuotaBytes(request.quotaBytes());

        quotaService.invalidateGroupMembersQuota(groupId);

        return GroupResponses.Response.fromEntity(group);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN')")
    public GroupResponses.Response updateGroup(Long groupId, GroupRequests.Update request) {
        Group group = findGroupOrThrow(groupId);
        group.setName(request.name());
        return GroupResponses.Response.fromEntity(group);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN')")
    public void deleteGroup(Long groupId) {
        Group group = findGroupOrThrow(groupId);
        groupRepository.delete(group);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public List<GroupResponses.Response> searchAllGroups(String query) {
        return groupRepository.findByNameContainingIgnoreCase(query)
                .stream()
                .map(GroupResponses.Response::fromEntity)
                .toList();
    }

    private Group findGroupOrThrow(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new ApiException(ErrorCode.GROUP_NOT_FOUND));
    }
}
