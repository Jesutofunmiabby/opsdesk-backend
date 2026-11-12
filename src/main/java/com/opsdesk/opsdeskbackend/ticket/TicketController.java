package com.opsdesk.opsdeskbackend.ticket;

import java.net.URI;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

import com.opsdesk.opsdeskbackend.common.ErrorResponse;
import com.opsdesk.opsdeskbackend.common.PageResponse;
import com.opsdesk.opsdeskbackend.common.QueryParams;
import com.opsdesk.opsdeskbackend.common.ValidationException;
import com.opsdesk.opsdeskbackend.security.AuthUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

// The @Operation, @Parameter and @ApiResponse annotations only describe the
// endpoints in the API documentation (Swagger). They must match
// API_CONTRACT.md; 401 and 500 are added to every endpoint in OpenApiConfig.
@RestController
@RequestMapping("/api/v1/tickets")
@Tag(name = "Tickets")
public class TicketController {

    // Fields the list can be sorted by. priority and status are left out: they
    // are stored as text, so they would sort alphabetically (HIGH, LOW, MEDIUM).
    private static final List<String> SORTABLE = List.of("id", "title", "createdAt", "updatedAt");

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    // Parameters are read as text so every bad one can be reported together.
    // Every method gets the logged-in user from their token with
    // @AuthenticationPrincipal; the service uses it for the ownership rules.
    @GetMapping
    @Operation(summary = "List tickets",
            description = "One page of tickets. Filters combine (status=OPEN&priority=HIGH means both). "
                    + "A USER only ever gets the tickets they created, whatever the filters say.")
    @ApiResponse(responseCode = "200", description = "One page of tickets.")
    @ApiResponse(responseCode = "400", description = "A parameter is out of range or not a known value. "
            + "fields has one message per bad parameter.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public PageResponse<TicketResponse> list(
            @AuthenticationPrincipal AuthUser actor,
            @Parameter(description = "The page, counting from 0.",
                    schema = @Schema(type = "integer", minimum = "0", defaultValue = "0"))
            @RequestParam(required = false) String page,
            @Parameter(description = "How many tickets on a page.",
                    schema = @Schema(type = "integer", minimum = "1", maximum = "100", defaultValue = "20"))
            @RequestParam(required = false) String size,
            @Parameter(description = "Only tickets with this status.",
                    schema = @Schema(implementation = TicketStatus.class))
            @RequestParam(required = false) String status,
            @Parameter(description = "Only tickets with this priority.",
                    schema = @Schema(implementation = TicketPriority.class))
            @RequestParam(required = false) String priority,
            @Parameter(description = "Only tickets assigned to the user with this id.",
                    schema = @Schema(type = "integer", format = "int64"))
            @RequestParam(required = false) String assigneeId,
            @Parameter(description = "Tickets whose title contains this text, ignoring capitals and spaces "
                    + "at either end. Empty means no search.", example = "vpn")
            @RequestParam(required = false) String q,
            @Parameter(description = "id, title, createdAt or updatedAt, optionally followed by ,asc or ,desc. "
                    + "Without a direction it is asc. Left out, the list is oldest first. Ties are in id order.",
                    schema = @Schema(type = "string", pattern = "^(id|title|createdAt|updatedAt)(,(asc|desc))?$"),
                    example = "createdAt,desc")
            @RequestParam(required = false) String sort) {
        QueryParams params = new QueryParams();
        int pageNumber = params.page(page);
        int pageSize = params.size(size);
        Sort order = params.sort(sort, SORTABLE);
        TicketStatus statusFilter = params.oneOf("status", status, TicketStatus.class,
                "Choose a status: OPEN, IN_PROGRESS, RESOLVED or CLOSED.");
        TicketPriority priorityFilter = params.oneOf("priority", priority, TicketPriority.class,
                "Choose a priority: LOW, MEDIUM or HIGH.");
        Long assigneeFilter = params.id("assigneeId", assigneeId);
        params.throwIfInvalid();

        TicketFilter filter = new TicketFilter(statusFilter, priorityFilter, assigneeFilter, q);
        return ticketService.findPage(actor, filter, pageNumber, pageSize, order).map(TicketResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one ticket")
    @ApiResponse(responseCode = "200", description = "The ticket.")
    @ApiResponse(responseCode = "404", description = "There is no ticket with this id, the id is not a whole "
            + "number, or a USER asked for someone else's ticket.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public TicketResponse get(@AuthenticationPrincipal AuthUser actor, @PathVariable long id) {
        return TicketResponse.from(ticketService.findVisible(actor, id));
    }

    @PostMapping
    @Operation(summary = "Create a ticket",
            description = "Every role can create tickets. The server sets the id, status OPEN, the dates, "
                    + "and the creator from the login.")
    @ApiResponse(responseCode = "201", description = "Created. The new ticket.",
            headers = @Header(name = "Location", description = "The new ticket's URL, such as /api/v1/tickets/26.",
                    schema = @Schema(type = "string")))
    @ApiResponse(responseCode = "400", description = "One or more fields break the rules (every one is listed "
            + "in fields), or the body is not a JSON object.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<TicketResponse> create(
            @AuthenticationPrincipal AuthUser actor,
            @Valid @RequestBody CreateTicketRequest request) {
        Ticket ticket = ticketService.create(actor, request);
        URI location = URI.create("/api/v1/tickets/" + ticket.getId());
        return ResponseEntity.created(location).body(TicketResponse.from(ticket));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update a ticket",
            description = "Changes only the fields sent; at least one is needed. ADMIN and MANAGER may edit any "
                    + "ticket, an AGENT only tickets assigned to them, a USER only their own while it is OPEN. "
                    + "Checked in this order: 404, then 403, then the body.")
    @ApiResponse(responseCode = "200", description = "The whole updated ticket.")
    @ApiResponse(responseCode = "400", description = "A field breaks its rule, the body is not a JSON object, "
            + "or it has none of the four fields.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "The user can see the ticket but may not change it.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "There is no ticket with this id (or the user cannot see it).",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public TicketResponse update(
            @AuthenticationPrincipal AuthUser actor,
            @PathVariable long id,
            @Valid @RequestBody UpdateTicketRequest request,
            BindingResult validation) {
        // The contract checks for a missing ticket before the body, so the
        // validation result is collected here and reported after the 404 and
        // 403 checks. Someone who may not edit learns nothing about the body.
        ticketService.findEditable(actor, id);
        if (validation.hasErrors()) {
            throw ValidationException.fromFieldErrors(validation.getFieldErrors());
        }
        return TicketResponse.from(ticketService.update(actor, id, request));
    }

    @PatchMapping("/{id}/assignee")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Assign a ticket", description = "ADMIN and MANAGER only. The 403 is checked before the 404.")
    @ApiResponse(responseCode = "200", description = "The whole updated ticket.")
    @ApiResponse(responseCode = "400", description = "There is no user with that id, or the user is a USER.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "The caller is an AGENT or a USER.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "There is no ticket with this id.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public TicketResponse assign(@PathVariable long id, @RequestBody AssignTicketRequest request) {
        return TicketResponse.from(ticketService.assign(id, request.assigneeId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a ticket",
            description = "ADMIN only. Permanent; the id is never given to another ticket. "
                    + "The 403 is checked before the 404.")
    @ApiResponse(responseCode = "204", description = "Deleted. No body.")
    @ApiResponse(responseCode = "403", description = "The caller is not an ADMIN.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "There is no ticket with this id, including one already deleted.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> delete(@PathVariable long id) {
        ticketService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
