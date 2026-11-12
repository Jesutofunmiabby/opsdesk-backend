package com.opsdesk.opsdeskbackend.ticket;

import com.opsdesk.opsdeskbackend.common.Text;
import com.opsdesk.opsdeskbackend.common.TextOnlyDeserializer;

import tools.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Every field is optional: a field left out of the JSON is null and stays as
// it was. A field that is sent follows the same rules as on create.
@Schema(description = "Send only the fields to change, at least one of them.")
public record UpdateTicketRequest(
        @Schema(minLength = 3, maxLength = 120, example = "Projector in room 4 has no sound")
        @Pattern(regexp = "(?s).+", message = "Enter a title.")
        @Size(min = 3, message = "The title must be at least 3 characters.")
        @Size(max = 120, message = "The title must be 120 characters or fewer.")
        @JsonDeserialize(using = TextOnlyDeserializer.class)
        String title,

        @Schema(minLength = 1, maxLength = 5000, example = "The speakers now work, but only at full volume.")
        @Pattern(regexp = "(?s).+", message = "Enter a description.")
        @Size(min = 1, max = 5000, message = "The description must be 5000 characters or fewer.")
        @JsonDeserialize(using = TextOnlyDeserializer.class)
        String description,

        @Schema(allowableValues = {"LOW", "MEDIUM", "HIGH"}, example = "HIGH")
        @Pattern(regexp = "LOW|MEDIUM|HIGH", message = "Choose a priority: LOW, MEDIUM or HIGH.")
        @JsonDeserialize(using = TextOnlyDeserializer.class)
        String priority,

        @Schema(allowableValues = {"OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"},
                description = "Any of the four. Moves are not limited to the next status.", example = "IN_PROGRESS")
        @Pattern(regexp = "OPEN|IN_PROGRESS|RESOLVED|CLOSED",
                message = "Choose a status: OPEN, IN_PROGRESS, RESOLVED or CLOSED.")
        @JsonDeserialize(using = TextOnlyDeserializer.class)
        String status) {

    public UpdateTicketRequest {
        title = Text.trim(title);
        description = Text.trim(description);
    }

    public boolean hasNoFields() {
        return title == null && description == null && priority == null && status == null;
    }
}
