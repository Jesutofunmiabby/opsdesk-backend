package com.opsdesk.opsdeskbackend.common;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

// Reads query parameters as text and collects a message for each one that
// breaks its rule, so a request with several bad parameters hears about all
// of them at once. Call throwIfInvalid() after reading them all.
public class QueryParams {

    private static final Pattern WHOLE_NUMBER = Pattern.compile("\\d+");

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

    public void throwIfInvalid() {
        if (!problems.isEmpty()) {
            throw new ValidationException("Check the query parameters.", problems);
        }
    }
}
