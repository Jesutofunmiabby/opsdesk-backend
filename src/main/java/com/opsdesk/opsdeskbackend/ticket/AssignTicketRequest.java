package com.opsdesk.opsdeskbackend.ticket;

import io.swagger.v3.oas.annotations.media.Schema;

// The body of PATCH /tickets/{id}/assignee: a user's id, or null to unassign.
public record AssignTicketRequest(
        @Schema(types = {"integer", "null"}, format = "int64",
                description = "The id of a user who is an ADMIN, MANAGER or AGENT. null, or left out, unassigns.",
                example = "9")
        Long assigneeId) {
}
