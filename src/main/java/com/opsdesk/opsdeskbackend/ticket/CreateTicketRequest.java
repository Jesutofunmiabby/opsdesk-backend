package com.opsdesk.opsdeskbackend.ticket;

public record CreateTicketRequest(
        String title,
        String description,
        TicketPriority priority) {
}
