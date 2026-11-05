package com.opsdesk.opsdeskbackend.ticket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import com.opsdesk.opsdeskbackend.common.ValidationException;
import com.opsdesk.opsdeskbackend.security.AuthUser;
import com.opsdesk.opsdeskbackend.user.AppUser;
import com.opsdesk.opsdeskbackend.user.AppUserRepository;
import com.opsdesk.opsdeskbackend.user.UserRole;

// The ownership rules in TicketService. Ticket 1 was created by user 4 (a
// USER) and is assigned to user 3 (an AGENT).
@ExtendWith(MockitoExtension.class)
class TicketPermissionTest {

    private static final AuthUser ADMIN = new AuthUser(1L, UserRole.ADMIN);
    private static final AuthUser MANAGER = new AuthUser(2L, UserRole.MANAGER);
    private static final AuthUser ASSIGNED_AGENT = new AuthUser(3L, UserRole.AGENT);
    private static final AuthUser OTHER_AGENT = new AuthUser(30L, UserRole.AGENT);
    private static final AuthUser CREATOR = new AuthUser(4L, UserRole.USER);
    private static final AuthUser OTHER_USER = new AuthUser(40L, UserRole.USER);

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private AppUserRepository appUserRepository;

    private TicketService ticketService;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        ticketService = new TicketService(ticketRepository, appUserRepository);
        ticket = new Ticket();
        ticket.setId(1L);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setCreatedBy(user(4L, UserRole.USER));
        ticket.setAssignee(user(3L, UserRole.AGENT));
    }

    @Test
    void adminAndManagerCanEditAnyTicket() {
        assertTrue(ticketService.canEdit(ADMIN, ticket));
        assertTrue(ticketService.canEdit(MANAGER, ticket));
    }

    @Test
    void anAgentCanOnlyEditTicketsAssignedToThem() {
        assertTrue(ticketService.canEdit(ASSIGNED_AGENT, ticket));
        assertFalse(ticketService.canEdit(OTHER_AGENT, ticket));

        ticket.setAssignee(null);
        assertFalse(ticketService.canEdit(ASSIGNED_AGENT, ticket));
    }

    @Test
    void aUserCanOnlyEditTheirOwnTicketWhileItIsOpen() {
        assertTrue(ticketService.canEdit(CREATOR, ticket));
        assertFalse(ticketService.canEdit(OTHER_USER, ticket));

        ticket.setStatus(TicketStatus.IN_PROGRESS);
        assertFalse(ticketService.canEdit(CREATOR, ticket));
    }

    @Test
    void aUserCannotEditATicketWithNoCreator() {
        ticket.setCreatedBy(null);

        assertFalse(ticketService.canEdit(CREATOR, ticket));
    }

    @Test
    void someoneElsesTicketIsNotFoundForAUser() {
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        TicketNotFoundException e = assertThrows(TicketNotFoundException.class,
                () -> ticketService.findVisible(OTHER_USER, 1L));

        assertEquals("There is no ticket with the id \"1\".", e.getMessage());
        assertSame(ticket, ticketService.findVisible(CREATOR, 1L));
        assertSame(ticket, ticketService.findVisible(OTHER_AGENT, 1L));
    }

    @Test
    void updatingATicketYouCanSeeButNotEditIsDenied() {
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        assertThrows(AccessDeniedException.class, () -> ticketService.update(OTHER_AGENT, 1L,
                new UpdateTicketRequest(null, null, null, "CLOSED")));

        verify(ticketRepository, never()).save(any(Ticket.class));
        assertEquals(TicketStatus.OPEN, ticket.getStatus());
    }

    @Test
    void createSetsTheCreatorFromTheLogin() {
        AppUser creator = user(4L, UserRole.USER);
        when(appUserRepository.findById(4L)).thenReturn(Optional.of(creator));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(call -> call.getArgument(0));

        Ticket created = ticketService.create(CREATOR,
                new CreateTicketRequest("Printer jams on floor 2", "Every second page jams.", "LOW"));

        assertSame(creator, created.getCreatedBy());
    }

    @Test
    void onlyStaffCanBeAssigned() {
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(appUserRepository.findById(4L)).thenReturn(Optional.of(user(4L, UserRole.USER)));

        ValidationException e = assertThrows(ValidationException.class, () -> ticketService.assign(1L, 4L));

        assertEquals("Choose a user who is an ADMIN, MANAGER or AGENT.", e.getFields().get("assigneeId"));
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    private static AppUser user(long id, UserRole role) {
        AppUser user = new AppUser("Test User " + id, "user" + id + "@example.com", "", role, Instant.now());
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
