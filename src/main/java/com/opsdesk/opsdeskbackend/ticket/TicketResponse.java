package com.opsdesk.opsdeskbackend.ticket;

import com.opsdesk.opsdeskbackend.common.ApiDates;

public record TicketResponse(
        long id,
        String title,
        String description,
        TicketPriority priority,
        TicketStatus status,
        String createdAt,
        String updatedAt) {

    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getPriority(),
                ticket.getStatus(),
                ApiDates.format(ticket.getCreatedAt()),
                ApiDates.format(ticket.getUpdatedAt()));
    }
}
