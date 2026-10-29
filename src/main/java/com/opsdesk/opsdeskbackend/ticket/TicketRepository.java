package com.opsdesk.opsdeskbackend.ticket;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

// Keeps tickets in memory until the PostgreSQL version replaces it, so
// everything is lost when the app stops. Methods are synchronized because
// several requests can use the list at the same time.
@Repository
public class TicketRepository {

    private final List<Ticket> tickets = new ArrayList<>();

    // Counts up and never goes back, so a deleted ticket's id is never reused.
    private long lastId = 0;

    public synchronized List<Ticket> findAll() {
        return List.copyOf(tickets);
    }

    public synchronized Optional<Ticket> findById(long id) {
        return tickets.stream()
                .filter(ticket -> ticket.getId() == id)
                .findFirst();
    }

    public synchronized Ticket save(Ticket ticket) {
        if (ticket.getId() == null) {
            lastId++;
            ticket.setId(lastId);
            tickets.add(ticket);
        }
        return ticket;
    }

    public synchronized void delete(Ticket ticket) {
        tickets.remove(ticket);
    }
}
