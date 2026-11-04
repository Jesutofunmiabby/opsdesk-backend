package com.opsdesk.opsdeskbackend.ticket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

// Sends real JSON to TicketController, so these tests cover how the body is
// read as well as how it is checked. The service is a mock: no database.
@WebMvcTest(TicketController.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setTitle("Projector in room 4 has no sound");
        ticket.setDescription("The picture works but the speakers stay silent.");
        ticket.setPriority(TicketPriority.MEDIUM);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setCreatedAt(Instant.parse("2026-10-15T09:30:00Z"));
        ticket.setUpdatedAt(Instant.parse("2026-10-15T09:30:00Z"));
        when(ticketService.findById(1L)).thenReturn(ticket);
        when(ticketService.update(anyLong(), any(UpdateTicketRequest.class))).thenReturn(ticket);
        when(ticketService.create(any(CreateTicketRequest.class))).thenReturn(ticket);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"title\": null}",
        "{\"title\": null, \"status\": \"OPEN\"}",
        "{\"title\": 123}",
        "{\"title\": true}",
        "{\"title\": [\"Projector\"]}",
        "{\"title\": {\"text\": \"Projector\"}}"
    })
    void patchWithATitleThatIsNotTextIsRejected(String body) throws Exception {
        patchTicket(body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Check the highlighted fields."))
                .andExpect(jsonPath("$.fields.title").value("Enter a title."));
        verify(ticketService, never()).update(anyLong(), any(UpdateTicketRequest.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "99999", "false"})
    void patchWithADescriptionThatIsNotTextIsRejected(String value) throws Exception {
        patchTicket("{\"description\": " + value + "}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.description").value("Enter a description."));
        verify(ticketService, never()).update(anyLong(), any(UpdateTicketRequest.class));
    }

    @Test
    void patchWithAPriorityOrStatusThatIsNotTextIsRejected() throws Exception {
        patchTicket("{\"priority\": null, \"status\": 2}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.priority").value("Choose a priority: LOW, MEDIUM or HIGH."))
                .andExpect(jsonPath("$.fields.status")
                        .value("Choose a status: OPEN, IN_PROGRESS, RESOLVED or CLOSED."));
        verify(ticketService, never()).update(anyLong(), any(UpdateTicketRequest.class));
    }

    @Test
    void fieldsLeftOutOfAPatchAreNotChanged() throws Exception {
        patchTicket("{\"status\": \"IN_PROGRESS\"}")
                .andExpect(status().isOk());

        ArgumentCaptor<UpdateTicketRequest> sent = ArgumentCaptor.forClass(UpdateTicketRequest.class);
        verify(ticketService).update(anyLong(), sent.capture());
        assertNull(sent.getValue().title());
        assertNull(sent.getValue().description());
        assertNull(sent.getValue().priority());
        assertEquals("IN_PROGRESS", sent.getValue().status());
    }

    @Test
    void postWithATitleAndDescriptionThatAreNotTextIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": 123, \"description\": true, \"priority\": \"LOW\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.title").value("Enter a title."))
                .andExpect(jsonPath("$.fields.description").value("Enter a description."));
        verify(ticketService, never()).create(any(CreateTicketRequest.class));
    }

    @Test
    void postWithTextFieldsStillCreatesTheTicket() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Projector in room 4 has no sound\","
                                + " \"description\": \"The speakers stay silent.\", \"priority\": \"LOW\"}"))
                .andExpect(status().isCreated());
    }

    private ResultActions patchTicket(String body) throws Exception {
        return mockMvc.perform(patch("/api/v1/tickets/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
