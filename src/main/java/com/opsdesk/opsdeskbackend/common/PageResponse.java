package com.opsdesk.opsdeskbackend.common;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

// One page of a list, in the shape every list endpoint returns.
public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalItems,
        int totalPages) {

    // The contract lists items oldest first.
    private static final Sort OLDEST_FIRST = Sort.by("id");

    // Runs a paged query, oldest first. A page past the end gives the last
    // page instead of an empty one. page and size must already be checked.
    public static <T> PageResponse<T> fetch(int page, int size, Function<Pageable, Page<T>> query) {
        Page<T> result = query.apply(PageRequest.of(page, size, OLDEST_FIRST));
        int lastPage = Math.max(0, result.getTotalPages() - 1);
        if (page > lastPage) {
            result = query.apply(PageRequest.of(lastPage, size, OLDEST_FIRST));
        }
        return from(result);
    }

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
