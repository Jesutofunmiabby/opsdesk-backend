package com.opsdesk.opsdeskbackend.common;

import java.util.Map;

// A request that breaks the contract's rules. Becomes 400 VALIDATION_ERROR.
// fields holds one message per field or query parameter, or is null when the
// problem is not about one field.
public class ValidationException extends RuntimeException {

    private final Map<String, String> fields;

    public ValidationException(String message) {
        this(message, null);
    }

    public ValidationException(String message, Map<String, String> fields) {
        super(message);
        this.fields = fields;
    }

    public Map<String, String> getFields() {
        return fields;
    }
}
