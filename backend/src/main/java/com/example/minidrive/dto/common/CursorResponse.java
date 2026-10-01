package com.example.minidrive.dto.common;

import com.example.minidrive.dto.file.FileResponses;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public record CursorResponse<T, K>(
        List<T> items,
        K nextCursor,
        boolean hasNext,
        int pageSize
) {
    public record CursorDto(
            String value,
            UUID uuid
    ) {}

    public static <T, K> CursorResponse<T, K> of(List<T> fetchedItems, int requestedSize, Function<T, K> cursorExtractor) {
        boolean hasNext = fetchedItems.size() > requestedSize;

        List<T> items = hasNext ? fetchedItems.subList(0, requestedSize) : fetchedItems;

        K nextCursor = hasNext ? cursorExtractor.apply(items.getLast()) : null;

        return new CursorResponse<>(items, nextCursor, hasNext, requestedSize);
    }

    public static CursorResponse<FileResponses.Response, CursorDto> of() {
        return new CursorResponse<>(List.of(), null, false, 0);
    }
}
