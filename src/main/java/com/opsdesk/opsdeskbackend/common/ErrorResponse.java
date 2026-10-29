package com.opsdesk.opsdeskbackend.common;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

// The error shape every endpoint uses. fields is left out of the JSON when
// there are no per-field messages.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String error,
        String message,
        Map<String, String> fields) {

    public ErrorResponse(String error, String message) {
        this(error, message, null);
    }
}
