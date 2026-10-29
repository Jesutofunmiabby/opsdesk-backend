package com.opsdesk.opsdeskbackend.ticket;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.opsdesk.opsdeskbackend.common.PageResponse;
import com.opsdesk.opsdeskbackend.common.QueryParams;
import com.opsdesk.opsdeskbackend.common.ValidationException;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    // Parameters are read as text so every bad one can be reported together.
    @GetMapping
    public PageResponse<TicketResponse> list(
            @RequestParam(required = false) String page,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String search) {
        QueryParams params = new QueryParams();
        int pageNumber = params.page(page);
        int pageSize = params.size(size);
        TicketStatus statusFilter = params.oneOf("status", status, TicketStatus.class,
                "Choose a status: OPEN, IN_PROGRESS, RESOLVED or CLOSED.");
        TicketPriority priorityFilter = params.oneOf("priority", priority, TicketPriority.class,
                "Choose a priority: LOW, MEDIUM or HIGH.");
        params.throwIfInvalid();

        TicketFilter filter = new TicketFilter(statusFilter, priorityFilter, search);
        return ticketService.findPage(filter, pageNumber, pageSize).map(TicketResponse::from);
    }

    @GetMapping("/{id}")
    public TicketResponse get(@PathVariable long id) {
        return TicketResponse.from(ticketService.findById(id));
    }

    @PostMapping
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody CreateTicketRequest request) {
        Ticket ticket = ticketService.create(request);
        URI location = URI.create("/api/v1/tickets/" + ticket.getId());
        return ResponseEntity.created(location).body(TicketResponse.from(ticket));
    }

    @PatchMapping("/{id}")
    public TicketResponse update(
            @PathVariable long id,
            @Valid @RequestBody UpdateTicketRequest request,
            BindingResult validation) {
        // The contract checks for a missing ticket before the body, so the
        // validation result is collected here and reported after the 404 check.
        ticketService.findById(id);
        if (validation.hasErrors()) {
            throw ValidationException.fromFieldErrors(validation.getFieldErrors());
        }
        return TicketResponse.from(ticketService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        ticketService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
