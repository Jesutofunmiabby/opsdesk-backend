package com.opsdesk.opsdeskbackend.ticket;

import com.opsdesk.opsdeskbackend.common.Text;
import com.opsdesk.opsdeskbackend.common.TextOnlyDeserializer;

import tools.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// @Schema only describes the field in the API documentation; the checks are
// the validation annotations. Two @Size checks on one field (each with its own
// message) do not show up in the docs, so the limits are repeated in @Schema.
public record CreateTicketRequest(
        @Schema(requiredMode = RequiredMode.REQUIRED, minLength = 3, maxLength = 120,
                description = "Trimmed before it is checked.", example = "Projector in room 4 has no sound")
        @NotBlank(message = "Enter a title.")
        @Size(min = 3, message = "The title must be at least 3 characters.")
        @Size(max = 120, message = "The title must be 120 characters or fewer.")
        @JsonDeserialize(using = TextOnlyDeserializer.class)
        String title,

        @Schema(requiredMode = RequiredMode.REQUIRED, minLength = 1, maxLength = 5000,
                description = "Trimmed before it is checked. Line breaks are kept.",
                example = "The picture works but the speakers stay silent.")
        @NotBlank(message = "Enter a description.")
        @Size(min = 1, max = 5000, message = "The description must be 5000 characters or fewer.")
        @JsonDeserialize(using = TextOnlyDeserializer.class)
        String description,

        @Schema(requiredMode = RequiredMode.REQUIRED, allowableValues = {"LOW", "MEDIUM", "HIGH"},
                example = "MEDIUM")
        @NotNull(message = "Choose a priority: LOW, MEDIUM or HIGH.")
        @Pattern(regexp = "LOW|MEDIUM|HIGH", message = "Choose a priority: LOW, MEDIUM or HIGH.")
        @JsonDeserialize(using = TextOnlyDeserializer.class)
        String priority) {

    // Runs before validation: the contract trims text before checking it, so
    // "  ab  " is too short.
    public CreateTicketRequest {
        title = Text.trim(title);
        description = Text.trim(description);
    }
}
