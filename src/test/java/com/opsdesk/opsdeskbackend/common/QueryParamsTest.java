package com.opsdesk.opsdeskbackend.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Sort;

class QueryParamsTest {

    private static final List<String> SORTABLE = List.of("id", "title", "createdAt", "updatedAt");

    private final QueryParams params = new QueryParams();

    @Test
    void noSortMeansOldestFirst() {
        assertEquals(Sort.by("id"), params.sort(null, SORTABLE));
    }

    @Test
    void aSortWithADirectionIsUsedWithIdToBreakTies() {
        assertEquals(Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id")),
                params.sort("createdAt,desc", SORTABLE));
    }

    @Test
    void aSortWithoutADirectionIsAscending() {
        assertEquals(Sort.by("title").and(Sort.by("id")), params.sort("title", SORTABLE));
        assertEquals(Sort.by(Sort.Direction.DESC, "id"), params.sort("id,desc", SORTABLE));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "priority", "createdAt,up", "createdAt,desc,id", "title,", "CREATEDAT"})
    void aBadSortIsReported(String sort) {
        params.sort(sort, SORTABLE);

        ValidationException e = assertThrows(ValidationException.class, params::throwIfInvalid);
        assertEquals("sort must be id, title, createdAt, updatedAt, optionally followed by ,asc or ,desc.",
                e.getFields().get("sort"));
    }

    @Test
    void anIdIsAWholeNumberOrLeftOut() {
        assertNull(params.id("assigneeId", null));
        assertEquals(9L, params.id("assigneeId", "9"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "-1", "1.5", "99999999999999999999"})
    void aBadIdIsReported(String id) {
        params.id("assigneeId", id);

        ValidationException e = assertThrows(ValidationException.class, params::throwIfInvalid);
        assertEquals("assigneeId must be a whole number.", e.getFields().get("assigneeId"));
    }
}
