package com.opsdesk.opsdeskbackend.ticket;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.opsdesk.opsdeskbackend.common.PageResponse;
import com.opsdesk.opsdeskbackend.common.ValidationException;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    // page and size have already been checked against the contract's ranges.
    public PageResponse<Ticket> findPage(TicketFilter filter, int page, int size) {
        Specification<Ticket> where = filter.toSpecification();
        return PageResponse.fetch(page, size, pageable -> ticketRepository.findAll(where, pageable));
    }

    public Ticket findById(long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(String.valueOf(id)));
    }

    public Ticket create(CreateTicketRequest request) {
        Instant now = now();
        Ticket ticket = new Ticket();
        ticket.setTitle(request.title());
        ticket.setDescription(request.description());
        ticket.setPriority(TicketPriority.valueOf(request.priority()));
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setCreatedAt(now);
        ticket.setUpdatedAt(now);
        return ticketRepository.save(ticket);
    }

    public Ticket update(long id, UpdateTicketRequest request) {
        Ticket ticket = findById(id);
        if (request.hasNoFields()) {
            throw new ValidationException("Send at least one of title, description, priority or status.");
        }
        if (request.title() != null) {
            ticket.setTitle(request.title());
        }
        if (request.description() != null) {
            ticket.setDescription(request.description());
        }
        if (request.priority() != null) {
            ticket.setPriority(TicketPriority.valueOf(request.priority()));
        }
        if (request.status() != null) {
            ticket.setStatus(TicketStatus.valueOf(request.status()));
        }
        ticket.setUpdatedAt(now());
        return ticketRepository.save(ticket);
    }

    public void delete(long id) {
        ticketRepository.delete(findById(id));
    }

    // The contract's dates have millisecond precision.
    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }
}
