package com.opsdesk.opsdeskbackend.common;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.data.domain.Sort;

// Reads query parameters as text and collects a message for each one that
// breaks its rule, so a request with several bad parameters hears about all
// of them at once. Call throwIfInvalid() after reading them all.
public class QueryParams {

    private static final Pattern WHOLE_NUMBER = Pattern.compile("\\d+");
    private static final List<String> DIRECTIONS = List.of("asc", "desc");

    private final Map<String, String> problems = new LinkedHashMap<>();

    public int page(String text) {
        if (text == null) {
            return 0;
        }
        if (!WHOLE_NUMBER.matcher(text).matches()) {
            problems.put("page", "page must be a whole number of 0 or more.");
            return 0;
        }
        // Too big for an int means far past the last page, which gives the
        // last page.
        return text.length() > 9 ? Integer.MAX_VALUE : Integer.parseInt(text);
    }

    public int size(String text) {
        if (text == null) {
            return 20;
        }
        if (!WHOLE_NUMBER.matcher(text).matches() || text.length() > 3
                || Integer.parseInt(text) < 1 || Integer.parseInt(text) > 100) {
            problems.put("size", "size must be a whole number from 1 to 100.");
            return 20;
        }
        return Integer.parseInt(text);
    }

    // An optional choice such as status=OPEN. Left out means no filter; sent,
    // it must be one of the choices, so status= with nothing after it is a
    // bad value, as in the contract and the frontend's mock API.
    public <E extends Enum<E>> E oneOf(String name, String text, Class<E> type, String message) {
        if (text == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, text);
        } catch (IllegalArgumentException e) {
            problems.put(name, message);
            return null;
        }
    }

    // An optional id such as assigneeId=3. Left out means no filter.
    public Long id(String name, String text) {
        if (text == null) {
            return null;
        }
        if (!WHOLE_NUMBER.matcher(text).matches() || text.length() > 18) {
            problems.put(name, name + " must be a whole number.");
            return null;
        }
        return Long.parseLong(text);
    }

    // sort=field or sort=field,asc or sort=field,desc, where field is one of
    // the allowed names. Left out, the list is oldest first (by id). Ties are
    // always broken by id, so the same item never shows up on two pages.
    public Sort sort(String text, List<String> allowed) {
        Sort byId = Sort.by("id");
        if (text == null) {
            return byId;
        }
        String[] parts = text.split(",", -1);
        String field = parts[0];
        String direction = parts.length == 2 ? parts[1] : "asc";
        if (parts.length > 2 || !allowed.contains(field) || !DIRECTIONS.contains(direction)) {
            problems.put("sort", "sort must be " + String.join(", ", allowed)
                    + ", optionally followed by ,asc or ,desc.");
            return byId;
        }
        Sort requested = Sort.by(Sort.Direction.fromString(direction), field);
        return field.equals("id") ? requested : requested.and(byId);
    }

    public void throwIfInvalid() {
        if (!problems.isEmpty()) {
            throw new ValidationException("Check the query parameters.", problems);
        }
    }
}
