package com.opsdesk.opsdeskbackend.ticket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// Tests TicketService on its own: the repository is a Mockito mock, so no
// database or Spring is needed.
@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketService(ticketRepository);
    }

    @Test
    void createSavesAnOpenTicket() {
        // The mock hands back whatever ticket it is asked to save.
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(call -> call.getArgument(0));
        CreateTicketRequest request = new CreateTicketRequest(
                "Projector in room 4 has no sound",
                "The picture works but the speakers stay silent.",
                "MEDIUM");

        Ticket ticket = ticketService.create(request);

        assertEquals("Projector in room 4 has no sound", ticket.getTitle());
        assertEquals(TicketPriority.MEDIUM, ticket.getPriority());
        assertEquals(TicketStatus.OPEN, ticket.getStatus());
        assertNotNull(ticket.getCreatedAt());
        assertEquals(ticket.getCreatedAt(), ticket.getUpdatedAt());
        verify(ticketRepository).save(ticket);
    }

    @Test
    void deletingAMissingTicketThrowsNotFound() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        TicketNotFoundException e = assertThrows(TicketNotFoundException.class,
                () -> ticketService.delete(99));

        assertEquals("There is no ticket with the id \"99\".", e.getMessage());
        verify(ticketRepository, never()).delete(any(Ticket.class));
    }
}
