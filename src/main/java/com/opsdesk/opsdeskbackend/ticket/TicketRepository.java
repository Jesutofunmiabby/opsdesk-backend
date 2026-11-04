package com.opsdesk.opsdeskbackend.ticket;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

// Spring Data writes the code for this interface when the app starts:
// findAll, findById, save, delete and more, all backed by PostgreSQL.
// JpaSpecificationExecutor adds findAll(Specification, Pageable) for the
// list filters, which can be combined in any way.
public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    // @EntityGraph loads each ticket's assignee and creator in the same query,
    // with a join, because the ticket JSON shows their names. Without it,
    // Hibernate runs one more query per user on the page: the N+1 problem.
    // The project is not shown, so it is not loaded.
    @Override
    @EntityGraph(attributePaths = {"assignee", "createdBy"})
    Page<Ticket> findAll(Specification<Ticket> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"assignee", "createdBy"})
    Optional<Ticket> findById(Long id);
}
