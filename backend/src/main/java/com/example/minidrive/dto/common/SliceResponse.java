package com.example.minidrive.dto.common;

import org.springframework.data.domain.Slice;

import java.util.List;

public record SliceResponse<T>(
        List<T> items,
        int currentPage,
        int sizePage,
        boolean hasNext,
        boolean hasPrevious
) {
    public static <T> SliceResponse<T> of(Slice<T> slice) {
        return new SliceResponse<>(
                slice.getContent(),
                slice.getNumber(),
                slice.getSize(),
                slice.hasNext(),
                slice.hasPrevious()
        );
    }
}
