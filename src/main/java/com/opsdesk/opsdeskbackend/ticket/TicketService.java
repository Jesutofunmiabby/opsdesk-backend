package com.opsdesk.opsdeskbackend.ticket;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.opsdesk.opsdeskbackend.common.PageResponse;
import com.opsdesk.opsdeskbackend.common.ValidationException;
import com.opsdesk.opsdeskbackend.security.AuthUser;
import com.opsdesk.opsdeskbackend.user.AppUser;
import com.opsdesk.opsdeskbackend.user.AppUserRepository;
import com.opsdesk.opsdeskbackend.user.UserRole;

// The ticket rules, including who may see and change which ticket. Role-only
// rules (only an ADMIN deletes) are on the controller; rules that depend on
// the ticket itself (whose it is, who it is assigned to) are here.
@Service
public class TicketService {

    private static final String NOT_ASSIGNABLE = "Choose a user who is an ADMIN, MANAGER or AGENT.";

    private final TicketRepository ticketRepository;
    private final AppUserRepository appUserRepository;

    public TicketService(TicketRepository ticketRepository, AppUserRepository appUserRepository) {
        this.ticketRepository = ticketRepository;
        this.appUserRepository = appUserRepository;
    }

    // page and size have already been checked against the contract's ranges.
    // A USER only ever gets the tickets they created, whatever the filters say.
    public PageResponse<Ticket> findPage(AuthUser actor, TicketFilter filter, int page, int size) {
        Specification<Ticket> where = filter.toSpecification();
        if (actor.role() == UserRole.USER) {
            where = where.and((root, query, cb) -> cb.equal(root.get("createdBy").get("id"), actor.id()));
        }
        Specification<Ticket> visible = where;
        return PageResponse.fetch(page, size, pageable -> ticketRepository.findAll(visible, pageable));
    }

    // Someone else's ticket is "not found" for a USER, the same as a ticket that
    // does not exist, so a USER cannot find out which ids are in use.
    public Ticket findVisible(AuthUser actor, long id) {
        Ticket ticket = findById(id);
        if (actor.role() == UserRole.USER && !isCreatedBy(ticket, actor)) {
            throw new TicketNotFoundException(String.valueOf(id));
        }
        return ticket;
    }

    // 404 if the actor cannot see the ticket, 403 if they can see it but not
    // change it.
    public Ticket findEditable(AuthUser actor, long id) {
        Ticket ticket = findVisible(actor, id);
        if (!canEdit(actor, ticket)) {
            throw new AccessDeniedException("Not allowed to edit ticket " + id);
        }
        return ticket;
    }

    // The "Edit ticket" row of the permission table in the README.
    public boolean canEdit(AuthUser actor, Ticket ticket) {
        return switch (actor.role()) {
            case ADMIN, MANAGER -> true;
            case AGENT -> isAssignedTo(ticket, actor);
            case USER -> isCreatedBy(ticket, actor) && ticket.getStatus() == TicketStatus.OPEN;
        };
    }

    public Ticket findById(long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(String.valueOf(id)));
    }

    // The creator always comes from the login, never from the request body.
    public Ticket create(AuthUser actor, CreateTicketRequest request) {
        Instant now = now();
        Ticket ticket = new Ticket();
        ticket.setTitle(request.title());
        ticket.setDescription(request.description());
        ticket.setPriority(TicketPriority.valueOf(request.priority()));
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setCreatedBy(appUserRepository.getReferenceById(actor.id()));
        ticket.setCreatedAt(now);
        ticket.setUpdatedAt(now);
        return ticketRepository.save(ticket);
    }

    public Ticket update(AuthUser actor, long id, UpdateTicketRequest request) {
        Ticket ticket = findEditable(actor, id);
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

    // Only staff can be assigned. A null assigneeId unassigns the ticket.
    public Ticket assign(long id, Long assigneeId) {
        Ticket ticket = findById(id);
        AppUser assignee = null;
        if (assigneeId != null) {
            assignee = appUserRepository.findById(assigneeId)
                    .filter(user -> user.getRole() != UserRole.USER)
                    .orElseThrow(() -> new ValidationException("Check the highlighted fields.",
                            Map.of("assigneeId", NOT_ASSIGNABLE)));
        }
        ticket.setAssignee(assignee);
        ticket.setUpdatedAt(now());
        return ticketRepository.save(ticket);
    }

    public void delete(long id) {
        ticketRepository.delete(findById(id));
    }

    private static boolean isCreatedBy(Ticket ticket, AuthUser actor) {
        return ticket.getCreatedBy() != null && ticket.getCreatedBy().getId() == actor.id();
    }

    private static boolean isAssignedTo(Ticket ticket, AuthUser actor) {
        return ticket.getAssignee() != null && ticket.getAssignee().getId() == actor.id();
    }

    // The contract's dates have millisecond precision.
    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }
}
