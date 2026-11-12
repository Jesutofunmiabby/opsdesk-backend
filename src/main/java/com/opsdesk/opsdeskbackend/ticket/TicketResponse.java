package com.opsdesk.opsdeskbackend.ticket;

import com.opsdesk.opsdeskbackend.common.ApiDates;
import com.opsdesk.opsdeskbackend.user.AppUser;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

// The @Schema notes are for the API documentation only. Every field is always
// in the JSON; the four about people are null when there is nobody.
@Schema(name = "Ticket")
public record TicketResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED, example = "4")
        long id,

        @Schema(requiredMode = RequiredMode.REQUIRED, minLength = 3, maxLength = 120,
                example = "Payroll export fails with a timeout error")
        String title,

        @Schema(requiredMode = RequiredMode.REQUIRED, minLength = 1, maxLength = 5000,
                description = "Line breaks are kept.",
                example = "The monthly payroll export stops after about a minute with a timeout error.")
        String description,

        @Schema(requiredMode = RequiredMode.REQUIRED)
        TicketPriority priority,

        @Schema(requiredMode = RequiredMode.REQUIRED, description = "A new ticket is always OPEN.")
        TicketStatus status,

        @Schema(requiredMode = RequiredMode.REQUIRED, format = "date-time",
                description = "When it was created, in UTC with milliseconds. Never changes.",
                example = "2026-09-04T09:00:00.000Z")
        String createdAt,

        @Schema(requiredMode = RequiredMode.REQUIRED, format = "date-time",
                description = "When it last changed. Equal to createdAt until the first change.",
                example = "2026-09-05T14:00:00.000Z")
        String updatedAt,

        @Schema(requiredMode = RequiredMode.REQUIRED, types = {"integer", "null"}, format = "int64",
                description = "The user working on it, or null if nobody is assigned.", example = "9")
        Long assigneeId,

        @Schema(requiredMode = RequiredMode.REQUIRED, types = {"string", "null"},
                description = "That user's name, or null.", example = "Thabo Molefe")
        String assigneeName,

        @Schema(requiredMode = RequiredMode.REQUIRED, types = {"integer", "null"}, format = "int64",
                description = "The user who created it, from their login. "
                        + "null for tickets created before logins existed.",
                example = "12")
        Long createdById,

        @Schema(requiredMode = RequiredMode.REQUIRED, types = {"string", "null"},
                description = "That user's name, or null.", example = "Lerato Kgosi")
        String createdByName) {

    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getPriority(),
                ticket.getStatus(),
                ApiDates.format(ticket.getCreatedAt()),
                ApiDates.format(ticket.getUpdatedAt()),
                idOf(ticket.getAssignee()),
                nameOf(ticket.getAssignee()),
                idOf(ticket.getCreatedBy()),
                nameOf(ticket.getCreatedBy()));
    }

    // Reading the id of a lazy user does not load the rest of it from the
    // database.
    private static Long idOf(AppUser user) {
        return user == null ? null : user.getId();
    }

    // Reading the name does: TicketRepository loads the users with the
    // tickets, so this is not a query per ticket.
    private static String nameOf(AppUser user) {
        return user == null ? null : user.getName();
    }
}
