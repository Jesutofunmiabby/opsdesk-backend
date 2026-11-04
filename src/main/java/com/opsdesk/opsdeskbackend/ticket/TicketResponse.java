package com.opsdesk.opsdeskbackend.ticket;

import com.opsdesk.opsdeskbackend.common.ApiDates;
import com.opsdesk.opsdeskbackend.user.AppUser;

public record TicketResponse(
        long id,
        String title,
        String description,
        TicketPriority priority,
        TicketStatus status,
        String createdAt,
        String updatedAt,
        Long assigneeId,
        Long createdById) {

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
                idOf(ticket.getCreatedBy()));
    }

    // Reading the id of a lazy user does not load the rest of it from the
    // database.
    private static Long idOf(AppUser user) {
        return user == null ? null : user.getId();
    }
}
