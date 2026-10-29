package com.opsdesk.opsdeskbackend.ticket;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

// Spring Data writes the code for this interface when the app starts:
// findAll, findById, save, delete and more, all backed by PostgreSQL.
// JpaSpecificationExecutor adds findAll(Specification, Pageable) for the
// list filters, which can be combined in any way.
public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {
}
