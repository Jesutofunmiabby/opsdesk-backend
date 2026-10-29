package com.opsdesk.opsdeskbackend.ticket;

import java.net.URI;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.opsdesk.opsdeskbackend.common.ErrorResponse;
import com.opsdesk.opsdeskbackend.common.InvalidQueryException;
import com.opsdesk.opsdeskbackend.common.PageResponse;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public PageResponse<TicketResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ticketService.findPage(page, size).map(TicketResponse::from);
    }

    @GetMapping("/{id}")
    public TicketResponse get(@PathVariable long id) {
        return TicketResponse.from(ticketService.findById(id));
    }

    @PostMapping
    public ResponseEntity<TicketResponse> create(@RequestBody CreateTicketRequest request) {
        Ticket ticket = ticketService.create(request);
        URI location = URI.create("/api/v1/tickets/" + ticket.getId());
        return ResponseEntity.created(location).body(TicketResponse.from(ticket));
    }

    @PatchMapping("/{id}")
    public TicketResponse update(@PathVariable long id, @RequestBody UpdateTicketRequest request) {
        return TicketResponse.from(ticketService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        ticketService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // These handlers only cover this controller. Issue #4 moves error
    // handling into one global handler.

    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(TicketNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(InvalidQueryException.class)
    public ResponseEntity<ErrorResponse> handleInvalidQuery(InvalidQueryException e) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("VALIDATION_ERROR", e.getMessage(), e.getFields()));
    }

    // A value that is not a whole number. The contract treats an id like
    // /tickets/abc as not found, and page=abc or size=abc as a bad query.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        if (e.getName().equals("id")) {
            return handleNotFound(new TicketNotFoundException(String.valueOf(e.getValue())));
        }
        String message = e.getName().equals("page")
                ? "page must be a whole number of 0 or more."
                : "size must be a whole number from 1 to 100.";
        return handleInvalidQuery(new InvalidQueryException(Map.of(e.getName(), message)));
    }
}
