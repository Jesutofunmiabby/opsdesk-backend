package com.opsdesk.opsdeskbackend.ticket;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.opsdesk.opsdeskbackend.common.PageResponse;
import com.opsdesk.opsdeskbackend.common.ValidationException;

@Service
public class TicketService {

    // The contract lists items oldest first.
    private static final Sort OLDEST_FIRST = Sort.by("id");

    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    // status is optional: null means tickets of every status.
    public PageResponse<Ticket> findPage(TicketStatus status, int page, int size) {
        Map<String, String> problems = new LinkedHashMap<>();
        if (page < 0) {
            problems.put("page", "page must be a whole number of 0 or more.");
        }
        if (size < 1 || size > 100) {
            problems.put("size", "size must be a whole number from 1 to 100.");
        }
        if (!problems.isEmpty()) {
            throw new ValidationException("Check the query parameters.", problems);
        }
        Page<Ticket> result = query(status, PageRequest.of(page, size, OLDEST_FIRST));
        // A page past the end gives the last page instead of an empty one.
        int lastPage = Math.max(0, result.getTotalPages() - 1);
        if (page > lastPage) {
            result = query(status, PageRequest.of(lastPage, size, OLDEST_FIRST));
        }
        return PageResponse.from(result);
    }

    private Page<Ticket> query(TicketStatus status, Pageable pageable) {
        if (status == null) {
            return ticketRepository.findAll(pageable);
        }
        return ticketRepository.findByStatus(status, pageable);
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
