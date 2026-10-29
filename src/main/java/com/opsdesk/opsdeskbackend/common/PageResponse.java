package com.opsdesk.opsdeskbackend.common;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

// One page of a list, in the shape every list endpoint returns.
public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalItems,
        int totalPages) {

    // An empty list still has one (empty) page: page 0.
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                Math.max(1, page.getTotalPages()));
    }

    public <R> PageResponse<R> map(Function<T, R> mapper) {
        return new PageResponse<>(items.stream().map(mapper).toList(), page, size, totalItems, totalPages);
    }
}
