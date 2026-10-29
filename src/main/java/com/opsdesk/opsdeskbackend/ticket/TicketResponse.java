package com.opsdesk.opsdeskbackend.ticket;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public record TicketResponse(
        long id,
        String title,
        String description,
        TicketPriority priority,
        TicketStatus status,
        String createdAt,
        String updatedAt) {

    // The contract's date format: UTC with milliseconds, e.g.
    // 2026-10-15T09:30:00.000Z.
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getPriority(),
                ticket.getStatus(),
                format(ticket.getCreatedAt()),
                format(ticket.getUpdatedAt()));
    }

    private static String format(Instant instant) {
        return DATE_FORMAT.format(instant);
    }
}
