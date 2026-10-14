package com.example.minidrive.service;

import com.example.minidrive.dto.file.FileResponses;
import com.example.minidrive.dto.share.ShareRequests;
import com.example.minidrive.dto.share.ShareResponses;
import com.example.minidrive.entity.Group;
import com.example.minidrive.entity.Node;
import com.example.minidrive.entity.Share;
import com.example.minidrive.entity.User;
import com.example.minidrive.enums.ErrorCode;
import com.example.minidrive.enums.NodeType;
import com.example.minidrive.enums.SharePermission;
import com.example.minidrive.exception.ApiException;
import com.example.minidrive.provider.StorageProviderFactory;
import com.example.minidrive.repository.GroupRepository;
import com.example.minidrive.repository.NodeRepository;
import com.example.minidrive.repository.ShareRepository;
import com.example.minidrive.repository.UserRepository;
import com.example.minidrive.security.JwtUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShareService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int MAX_RECIPIENTS_PER_TYPE = 50;

    private final NodeRepository nodeRepository;
    private final ShareRepository shareRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final StorageProviderFactory storageProviderFactory;
    private final Clock clock;

    @Transactional
    @PreAuthorize("@fileSecurity.canManageShares(#request.fileUuid(), #jwtUser)")
    public ShareResponses.Details updateShares(ShareRequests.Update request, JwtUserDetails jwtUser) {
        validateRequest(request);

        Node node = nodeRepository.findOwnedLiveNodeForUpdate(request.fileUuid(), jwtUser.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.FILE_NOT_FOUND));
        List<Share> activeShares = shareRepository.findActiveByNodeId(node.getId());

        if (request.linkEnabled() != null) {
            Share link = activeShares.stream()
                    .filter(share -> share.getShareToken() != null)
                    .findFirst()
                    .orElse(null);

            if (request.linkEnabled()) {
                if (link == null) {
                    link = newShare(node);
                    link.setShareToken(generateShareToken());
                    link.setPermissions(SharePermission.READ);
                    link.setExpiresAt(null);
                    shareRepository.save(link);
                }
            } else if (link != null) {
                link.setDeletedAt(clock.instant());
                shareRepository.save(link);
            }

            return getDetails(node, shareRepository.findActiveByNodeId(node.getId()));
        }

        List<User> users = findRequestedUsers(request.userUuids(), jwtUser);
        List<Group> groups = findRequestedGroups(request.groupUuids());
        Map<Long, Share> sharesByUser = activeShares.stream()
                .filter(share -> share.getRecipient() != null)
                .collect(Collectors.toMap(share -> share.getRecipient().getId(), Function.identity(), (first, ignored) -> first));
        Map<Long, Share> sharesByGroup = activeShares.stream()
                .filter(share -> share.getGroup() != null)
                .collect(Collectors.toMap(share -> share.getGroup().getId(), Function.identity(), (first, ignored) -> first));
        List<Share> changedShares = new ArrayList<>();
        for (User recipient : users) {
            Share share = sharesByUser.getOrDefault(recipient.getId(), newShare(node));
            share.setRecipient(recipient);
            share.setPermissions(request.permission());
            changedShares.add(share);
        }
        for (Group group : groups) {
            Share share = sharesByGroup.getOrDefault(group.getId(), newShare(node));
            share.setGroup(group);
            share.setPermissions(request.permission());
            changedShares.add(share);
        }

        shareRepository.saveAll(changedShares);
        return getDetails(node, shareRepository.findActiveByNodeId(node.getId()));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@fileSecurity.canManageShares(#nodeUuid, #jwtUser)")
    public ShareResponses.Details getShares(UUID nodeUuid, JwtUserDetails jwtUser) {
        Node node = nodeRepository.findByUuidAndOwner_IdAndDeletedAtIsNull(nodeUuid, jwtUser.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.FILE_NOT_FOUND));
        return getDetails(node, shareRepository.findActiveByNodeId(node.getId()));
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public void revokeShare(UUID shareUuid, JwtUserDetails jwtUser) {
        Share share = shareRepository.findByUuidAndDeletedAtIsNull(shareUuid)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
        Node node = nodeRepository.findOwnedLiveNodeForUpdate(
                        share.getNode().getUuid(),
                        jwtUser.getId()
                )
                .orElseThrow(() -> new ApiException(ErrorCode.ACCESS_DENIED));
        if (!share.getOwner().getId().equals(jwtUser.getId())
                || !node.getOwner().getId().equals(jwtUser.getId())) {
            throw new ApiException(ErrorCode.ACCESS_DENIED);
        }
        share.setDeletedAt(clock.instant());
    }

    @Transactional(readOnly = true)
    public ShareResponses.PublicNode getPublicFile(String token, UUID requestedFileUuid) {
        Share link = findActiveLinkOrThrow(token);
        Node root = link.getNode();
        UUID nodeUuid = requestedFileUuid == null ? root.getUuid() : requestedFileUuid;
        if (!nodeRepository.isLiveNodeInSubtree(root.getId(), nodeUuid)) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        Node node = nodeRepository.findByUuidAndDeletedAtIsNull(nodeUuid)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
        if (node.getNodeType() != NodeType.FOLDER && node.getNodeType() != NodeType.FILE) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        List<Node> children = node.getNodeType() == NodeType.FOLDER
                ? nodeRepository.findAllByParent_IdAndDeletedAtIsNullOrderByNameAsc(node.getId())
                : List.of();
        return ShareResponses.PublicNode.fromEntity(node, children, link.getPermissions());
    }

    @Transactional(readOnly = true)
    public FileResponses.DownloadResult downloadPublicFile(String token, UUID fileUuid) {
        Share link = findActiveLinkOrThrow(token);
        if (!nodeRepository.isLiveNodeInSubtree(link.getNode().getId(), fileUuid)) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        Node file = nodeRepository.findByUuidAndDeletedAtIsNull(fileUuid)
                .filter(node -> node.getNodeType() == NodeType.FILE)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
        return new FileResponses.DownloadResult(
                storageProviderFactory.getProvider(file.getStorage()).getFileStream(file.getStorageKey()),
                file.getName(),
                file.getMimeType() == null ? "application/octet-stream" : file.getMimeType(),
                file.getSizeBytes()
        );
    }

    private Share findActiveLinkOrThrow(String token) {
        Share link = shareRepository.findActiveLink(token)
                .filter(share -> share.getShareToken() != null)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
        if (link.getExpiresAt() != null && !link.getExpiresAt().isAfter(clock.instant())) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        if (link.getNode().getDeletedAt() != null) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return link;
    }

    private ShareResponses.Details getDetails(Node node, List<Share> shares) {
        List<ShareResponses.Recipient> recipients = shares.stream()
                .filter(share -> share.getShareToken() == null)
                .map(share -> share.getRecipient() != null
                        ? ShareResponses.Recipient.fromUser(share)
                        : ShareResponses.Recipient.fromGroup(share))
                .toList();
        Share link = shares.stream()
                .filter(share -> share.getShareToken() != null)
                .findFirst()
                .orElse(null);
        ShareResponses.PublicLink publicLink = link == null
                ? new ShareResponses.PublicLink(false, null, null)
                : new ShareResponses.PublicLink(true, link.getShareToken(), link.getPermissions());
        return new ShareResponses.Details(node.getUuid(), recipients, publicLink);
    }

    private List<User> findRequestedUsers(List<UUID> uuids, JwtUserDetails jwtUser) {
        if (uuids.isEmpty()) {
            return List.of();
        }
        List<User> users = userRepository.findAllByUuidInAndDeletedAtIsNull(uuids);
        if (users.size() != uuids.size()) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
        if (users.stream().anyMatch(user -> user.getId().equals(jwtUser.getId()))) {
            throw new ApiException(ErrorCode.INVALID_INPUT);
        }
        return users;
    }

    private List<Group> findRequestedGroups(List<UUID> uuids) {
        if (uuids.isEmpty()) {
            return List.of();
        }
        List<Group> groups = groupRepository.findAllByUuidIn(uuids);
        if (groups.size() != uuids.size()) {
            throw new ApiException(ErrorCode.GROUP_NOT_FOUND);
        }
        return groups;
    }

    private void validateRequest(ShareRequests.Update request) {
        boolean hasDirectRecipients = !request.userUuids().isEmpty() || !request.groupUuids().isEmpty();
        boolean invalidDirectRecipients = request.userUuids().size() > MAX_RECIPIENTS_PER_TYPE
                || request.groupUuids().size() > MAX_RECIPIENTS_PER_TYPE
                || hasDuplicates(request.userUuids())
                || hasDuplicates(request.groupUuids())
                || request.userUuids().stream().anyMatch(java.util.Objects::isNull)
                || request.groupUuids().stream().anyMatch(java.util.Objects::isNull);
        boolean invalidLinkRequest = request.linkEnabled() != null && hasDirectRecipients;
        boolean missingDirectShareData = request.linkEnabled() == null
                && (!hasDirectRecipients || request.permission() == null);

        if (invalidDirectRecipients || invalidLinkRequest || missingDirectShareData) {
            throw new ApiException(ErrorCode.INVALID_INPUT);
        }
    }

    private boolean hasDuplicates(List<UUID> uuids) {
        return new HashSet<>(uuids).size() != uuids.size();
    }

    private Share newShare(Node node) {
        return Share.builder()
                .node(node)
                .owner(node.getOwner())
                .build();
    }

    private String generateShareToken() {
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }
}
