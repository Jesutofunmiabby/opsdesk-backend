package com.opsdesk.opsdeskbackend.ticket;

import org.springframework.data.jpa.repository.JpaRepository;

// Spring Data writes the code for this interface when the app starts:
// findAll, findById, save, delete and more, all backed by PostgreSQL.
public interface TicketRepository extends JpaRepository<Ticket, Long> {
}
