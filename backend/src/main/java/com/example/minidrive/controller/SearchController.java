package com.example.minidrive.controller;

import com.example.minidrive.dto.share.ShareResponses;
import com.example.minidrive.enums.ErrorCode;
import com.example.minidrive.exception.ApiException;
import com.example.minidrive.security.JwtUserDetails;
import com.example.minidrive.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {
    private final SearchService searchService;
    private static final int MIN_QUERY_LENGTH = 3;

    @GetMapping("/share-targets")
    public ResponseEntity<ShareResponses.Targets> search(
            @RequestParam("query") String query,
            @AuthenticationPrincipal JwtUserDetails jwtUser
    ) {
        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.length() < MIN_QUERY_LENGTH) {
            throw new ApiException(ErrorCode.INVALID_INPUT);
        }

        return ResponseEntity.ok(searchService.shareTargets(normalizedQuery, jwtUser));
    }
}
