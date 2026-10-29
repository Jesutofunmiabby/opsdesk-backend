package com.opsdesk.opsdeskbackend.ticket;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Every field is optional: a field left out of the JSON is null and stays as
// it was. A field that is sent follows the same rules as on create.
public record UpdateTicketRequest(
        @Pattern(regexp = "(?s).+", message = "Enter a title.")
        @Size(min = 3, message = "The title must be at least 3 characters.")
        @Size(max = 120, message = "The title must be 120 characters or fewer.")
        String title,

        @Pattern(regexp = "(?s).+", message = "Enter a description.")
        @Size(max = 5000, message = "The description must be 5000 characters or fewer.")
        String description,

        @Pattern(regexp = "LOW|MEDIUM|HIGH", message = "Choose a priority: LOW, MEDIUM or HIGH.")
        String priority,

        @Pattern(regexp = "OPEN|IN_PROGRESS|RESOLVED|CLOSED",
                message = "Choose a status: OPEN, IN_PROGRESS, RESOLVED or CLOSED.")
        String status) {

    public UpdateTicketRequest {
        title = title == null ? null : title.strip();
        description = description == null ? null : description.strip();
    }

    public boolean hasNoFields() {
        return title == null && description == null && priority == null && status == null;
    }
}
