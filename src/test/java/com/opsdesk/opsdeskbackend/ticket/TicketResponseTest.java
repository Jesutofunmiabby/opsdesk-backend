package com.opsdesk.opsdeskbackend.ticket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.opsdesk.opsdeskbackend.user.AppUser;
import com.opsdesk.opsdeskbackend.user.UserRole;

class TicketResponseTest {

    @Test
    void showsTheIdAndNameOfTheAssigneeAndCreator() {
        Ticket ticket = ticket();
        ticket.setAssignee(user(9L, "Thabo Molefe", UserRole.AGENT));
        ticket.setCreatedBy(user(10L, "Amara Okafor", UserRole.USER));

        TicketResponse response = TicketResponse.from(ticket);

        assertEquals(9L, response.assigneeId());
        assertEquals("Thabo Molefe", response.assigneeName());
        assertEquals(10L, response.createdById());
        assertEquals("Amara Okafor", response.createdByName());
    }

    @Test
    void leavesThemNullWhenThereIsNoAssigneeOrCreator() {
        TicketResponse response = TicketResponse.from(ticket());

        assertNull(response.assigneeId());
        assertNull(response.assigneeName());
        assertNull(response.createdById());
        assertNull(response.createdByName());
    }

    private static Ticket ticket() {
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setTitle("Printer jams on floor 2");
        ticket.setDescription("Every second page jams.");
        ticket.setPriority(TicketPriority.LOW);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setCreatedAt(Instant.parse("2026-11-04T09:00:00Z"));
        ticket.setUpdatedAt(Instant.parse("2026-11-04T09:00:00Z"));
        return ticket;
    }

    private static AppUser user(long id, String name, UserRole role) {
        AppUser user = new AppUser(name, "user" + id + "@example.com", "", role, Instant.now());
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
