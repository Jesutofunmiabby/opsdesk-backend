package com.opsdesk.opsdeskbackend.common;

import java.util.List;
import java.util.function.Function;

// One page of a list, in the shape every list endpoint returns.
public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalItems,
        int totalPages) {

    // Cuts one page out of the full list. A page past the end gives the last
    // page, and an empty list still has one (empty) page: page 0.
    public static <T> PageResponse<T> of(List<T> all, int page, int size) {
        int totalPages = Math.max(1, (all.size() + size - 1) / size);
        int actualPage = Math.min(page, totalPages - 1);
        int from = actualPage * size;
        int to = Math.min(from + size, all.size());
        return new PageResponse<>(all.subList(from, to), actualPage, size, all.size(), totalPages);
    }

    public <R> PageResponse<R> map(Function<T, R> mapper) {
        return new PageResponse<>(items.stream().map(mapper).toList(), page, size, totalItems, totalPages);
    }
}
