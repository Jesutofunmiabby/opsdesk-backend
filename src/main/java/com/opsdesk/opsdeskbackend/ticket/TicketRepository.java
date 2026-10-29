package com.opsdesk.opsdeskbackend.ticket;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

// Spring Data writes the code for this interface when the app starts:
// findAll, findById, save, delete and more, all backed by PostgreSQL.
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // A derived query: Spring Data reads the method name and builds
    // SELECT ... FROM ticket WHERE status = ? with paging and sorting.
    Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);
}
