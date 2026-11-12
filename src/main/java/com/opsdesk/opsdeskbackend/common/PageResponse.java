package com.opsdesk.opsdeskbackend.common;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

// One page of a list, in the shape every list endpoint returns.
public record PageResponse<T>(
        @Schema(requiredMode = RequiredMode.REQUIRED,
                description = "The items on this page, oldest first (ascending id) unless sorted.")
        List<T> items,

        @Schema(requiredMode = RequiredMode.REQUIRED, minimum = "0",
                description = "The page these items are from, counting from 0. A page past the end gives the last page.",
                example = "0")
        int page,

        @Schema(requiredMode = RequiredMode.REQUIRED, minimum = "1", maximum = "100",
                description = "The page size used.", example = "20")
        int size,

        @Schema(requiredMode = RequiredMode.REQUIRED, minimum = "0",
                description = "How many items match, across all pages.", example = "3")
        long totalItems,

        @Schema(requiredMode = RequiredMode.REQUIRED, minimum = "1",
                description = "How many pages there are. Always at least 1, so an empty list still has page 0.",
                example = "1")
        int totalPages) {

    // The contract lists items oldest first.
    private static final Sort OLDEST_FIRST = Sort.by("id");

    // Runs a paged query, oldest first. A page past the end gives the last
    // page instead of an empty one. page and size must already be checked.
    public static <T> PageResponse<T> fetch(int page, int size, Function<Pageable, Page<T>> query) {
        return fetch(page, size, OLDEST_FIRST, query);
    }

    // The same, in the given order.
    public static <T> PageResponse<T> fetch(int page, int size, Sort sort, Function<Pageable, Page<T>> query) {
        Page<T> result = query.apply(PageRequest.of(page, size, sort));
        int lastPage = Math.max(0, result.getTotalPages() - 1);
        if (page > lastPage) {
            result = query.apply(PageRequest.of(lastPage, size, sort));
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
