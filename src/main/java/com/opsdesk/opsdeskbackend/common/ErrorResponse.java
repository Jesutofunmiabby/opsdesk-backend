package com.opsdesk.opsdeskbackend.common;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

// The error shape every endpoint uses. fields is left out of the JSON when
// there are no per-field messages.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED, description = "A fixed code, for code to check.",
                allowableValues = {"VALIDATION_ERROR", "UNAUTHENTICATED", "FORBIDDEN", "NOT_FOUND",
                        "METHOD_NOT_ALLOWED", "CONFLICT", "INTERNAL_ERROR"},
                example = "VALIDATION_ERROR")
        String error,

        @Schema(requiredMode = RequiredMode.REQUIRED, description = "A sentence a person can read.",
                example = "Check the highlighted fields.")
        String message,

        @Schema(requiredMode = RequiredMode.NOT_REQUIRED,
                description = "Only for VALIDATION_ERROR about particular fields: one message per field "
                        + "with a problem, keyed by the field's name.",
                example = "{\"title\": \"The title must be at least 3 characters.\"}")
        Map<String, String> fields) {

    public ErrorResponse(String error, String message) {
        this(error, message, null);
    }
}
