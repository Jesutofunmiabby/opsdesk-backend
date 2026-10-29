package com.opsdesk.opsdeskbackend.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @NotBlank(message = "Enter a title.")
        @Size(min = 3, message = "The title must be at least 3 characters.")
        @Size(max = 120, message = "The title must be 120 characters or fewer.")
        String title,

        @NotBlank(message = "Enter a description.")
        @Size(max = 5000, message = "The description must be 5000 characters or fewer.")
        String description,

        @NotNull(message = "Choose a priority: LOW, MEDIUM or HIGH.")
        @Pattern(regexp = "LOW|MEDIUM|HIGH", message = "Choose a priority: LOW, MEDIUM or HIGH.")
        String priority) {

    // Runs before validation: the contract trims text before checking it, so
    // "  ab  " is too short.
    public CreateTicketRequest {
        title = title == null ? null : title.strip();
        description = description == null ? null : description.strip();
    }
}
