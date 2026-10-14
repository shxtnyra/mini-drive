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

    public PageResponse<UserResponses.Summary> getUsersFromGroup(UUID groupUuid, Pageable pageable) {
        Group group = findGroupOrThrow(groupUuid);

        Page<UserResponses.Summary> users = userRepository.findByGroup(group.getId(), pageable)
                .map(UserResponses.Summary::fromEntity);

        return PageResponse.of(users);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN')")
    public void addUsersToGroup(UUID groupUuid, List<UUID> usersUuid) {
        Group group = findGroupOrThrow(groupUuid);

        List<User> users = userRepository.findAllByUuidIn(usersUuid);

        if (users.isEmpty()) {
            return;
        }

        List<Long> userIds = users.stream().map(User::getId).toList();
        groupRepository.addMembers(group.getId(), userIds);
        userIds.forEach(quotaService::invalidateUserQuota);
        group.setMembersCount(group.getMembersCount() + userIds.size());
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN')")
    public void removeUsersFromGroup(UUID groupUuid, List<UUID> usersUuid) {
        Group group = findGroupOrThrow(groupUuid);

        List<User> users = userRepository.findAllByUuidIn(usersUuid);

        if (users.isEmpty()) {
            return;
        }

        List<Long> userIds = users.stream().map(User::getId).toList();
        groupRepository.removeMembers(group.getId(), userIds);
        userIds.forEach(quotaService::invalidateUserQuota);
        group.setMembersCount(group.getMembersCount() - userIds.size());
    }

    @Transactional
    public GroupResponses.Response setGroupQuota(UUID groupUuid, GroupRequests.SetQuota request) {
        Group group = findGroupOrThrow(groupUuid);
        group.setQuotaBytes(request.quotaBytes());
        quotaService.invalidateGroupMembersQuota(group.getId());
        return GroupResponses.Response.fromEntity(group);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN')")
    public GroupResponses.Response updateGroup(UUID groupUuid, GroupRequests.Update request) {
        Group group = findGroupOrThrow(groupUuid);
        group.setName(request.name());
        return GroupResponses.Response.fromEntity(group);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN')")
    public void deleteGroup(UUID groupUuid) {
        Group group = findGroupOrThrow(groupUuid);
        groupRepository.delete(group);
    }

    private Group findGroupOrThrow(UUID groupUuid) {
        return groupRepository.findByUuid(groupUuid)
                .orElseThrow(() -> new ApiException(ErrorCode.GROUP_NOT_FOUND));
    }
}
