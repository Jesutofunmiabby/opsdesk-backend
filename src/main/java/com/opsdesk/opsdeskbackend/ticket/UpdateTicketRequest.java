package com.opsdesk.opsdeskbackend.ticket;

// Every field is optional: a field left out of the JSON is null and stays as
// it was.
public record UpdateTicketRequest(
        String title,
        String description,
        TicketPriority priority,
        TicketStatus status) {
}
