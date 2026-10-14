package com.example.minidrive.dto.share;

import com.example.minidrive.dto.file.FileResponses;
import com.example.minidrive.entity.Group;
import com.example.minidrive.entity.Node;
import com.example.minidrive.entity.Share;
import com.example.minidrive.entity.User;
import com.example.minidrive.enums.SharePermission;

import java.util.List;
import java.util.UUID;

public class ShareResponses {
    public record UserTarget(UUID uuid, String displayName, String username, String email) {
        public static UserTarget fromEntity(User user) {
            return new UserTarget(user.getUuid(), user.getDisplayName(), user.getUsername(), user.getEmail());
        }
    }

    public record GroupTarget(UUID uuid, String name, int membersCount) {
        public static GroupTarget fromEntity(Group group) {
            return new GroupTarget(group.getUuid(), group.getName(), group.getMembersCount());
        }
    }

    public record Targets(List<UserTarget> users, List<GroupTarget> groups) {}

    public record Recipient(
            UUID shareUuid,
            String type,
            UUID recipientUuid,
            String name,
            String email,
            SharePermission permission
    ) {
        public static Recipient fromUser(Share share) {
            User user = share.getRecipient();
            return new Recipient(share.getUuid(), "USER", user.getUuid(),
                    displayName(user), user.getEmail(), share.getPermissions());
        }

        public static Recipient fromGroup(Share share) {
            Group group = share.getGroup();
            return new Recipient(share.getUuid(), "GROUP", group.getUuid(),
                    group.getName(), null, share.getPermissions());
        }

        private static String displayName(User user) {
            return user.getDisplayName() == null || user.getDisplayName().isBlank()
                    ? user.getUsername()
                    : user.getDisplayName();
        }
    }

    public record PublicLink(boolean enabled, String token, SharePermission permission) {}

    public record Details(UUID nodeUuid, List<Recipient> recipients, PublicLink link) {}

    public record PublicNode(
            FileResponses.Response node,
            List<FileResponses.Response> children,
            SharePermission permission
    ) {
        public static PublicNode fromEntity(Node node, List<Node> children, SharePermission permission) {
            return new PublicNode(
                    FileResponses.Response.fromEntity(node),
                    children.stream().map(FileResponses.Response::fromEntity).toList(),
                    permission
            );
        }
    }
}
