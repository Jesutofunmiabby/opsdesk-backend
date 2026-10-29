package com.opsdesk.opsdeskbackend.common;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.validation.FieldError;

// A request that breaks the contract's rules. Becomes 400 VALIDATION_ERROR.
// fields holds one message per field or query parameter, or is null when the
// problem is not about one field.
public class ValidationException extends RuntimeException {

    // Checks that mean "missing or empty". When a field breaks one of these and
    // another rule too, the contract shows this message ("Enter a title.").
    private static final Set<String> MISSING_CHECKS = Set.of("NotBlank", "NotNull", "Pattern");

    private final Map<String, String> fields;

    public ValidationException(String message) {
        this(message, null);
    }

    public ValidationException(String message, Map<String, String> fields) {
        super(message);
        this.fields = fields;
    }

    // One message per field, as the contract asks, even when a field breaks
    // more than one rule.
    public static ValidationException fromFieldErrors(List<FieldError> errors) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError error : errors) {
            if (MISSING_CHECKS.contains(error.getCode()) || !fields.containsKey(error.getField())) {
                fields.put(error.getField(), error.getDefaultMessage());
            }
        }
        return new ValidationException("Check the highlighted fields.", fields);
    }

    public Map<String, String> getFields() {
        return fields;
    }
}
