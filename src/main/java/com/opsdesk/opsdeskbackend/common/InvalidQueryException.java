package com.opsdesk.opsdeskbackend.common;

import java.util.Map;

// A query parameter is out of range. fields holds one message per parameter.
public class InvalidQueryException extends RuntimeException {

    private final Map<String, String> fields;

    public InvalidQueryException(Map<String, String> fields) {
        super("Check the query parameters.");
        this.fields = fields;
    }

    public Map<String, String> getFields() {
        return fields;
    }
}
