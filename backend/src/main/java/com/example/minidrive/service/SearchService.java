package com.example.minidrive.service;

import com.example.minidrive.dto.share.ShareResponses;
import com.example.minidrive.entity.Group;
import com.example.minidrive.entity.User;
import com.example.minidrive.repository.GroupRepository;
import com.example.minidrive.repository.UserRepository;
import com.example.minidrive.security.JwtUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {
    private static final int MAX_RESULTS_PER_TYPE = 10;

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ShareResponses.Targets shareTargets(String query, JwtUserDetails currentUser) {

        var limit = PageRequest.of(0, MAX_RESULTS_PER_TYPE);
        List<User> users = userRepository.searchShareTargets(
                query,
                currentUser.getUuid(),
                limit
        );
        List<Group> groups = groupRepository.findByNameContainingIgnoreCaseOrderByNameAscUuidAsc(
                query,
                limit
        );

        return new ShareResponses.Targets(
                users.stream().map(ShareResponses.UserTarget::fromEntity).toList(),
                groups.stream().map(ShareResponses.GroupTarget::fromEntity).toList()
        );
    }
}
