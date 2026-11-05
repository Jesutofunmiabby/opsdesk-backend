package com.opsdesk.opsdeskbackend.ticket;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;
import com.opsdesk.opsdeskbackend.user.AppUser;
import com.opsdesk.opsdeskbackend.user.AppUserRepository;
import com.opsdesk.opsdeskbackend.user.UserRole;

// The whole application against the in-memory H2 database: real tokens from
// register and login, the real security chain, the real service and real SQL.
// Nothing is mocked. H2 has no tickets and no users who can log in at the
// start, so each test makes its own, with a fresh email so tests never clash.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TicketSecurityIntegrationTest {

    private static final String PASSWORD = "test-password-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void noTokenIs401() throws Exception {
        mockMvc.perform(get("/api/v1/tickets"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"));
    }

    @Test
    void aUserCannotDeleteATicket() throws Exception {
        String userToken = register();
        long ticketId = createTicket(userToken, "A USER's own ticket");

        mockMvc.perform(delete("/api/v1/tickets/" + ticketId).header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));

        assertTrue(ticketRepository.existsById(ticketId));
    }

    @Test
    void anAdminCanDeleteATicket() throws Exception {
        String userToken = register();
        long ticketId = createTicket(userToken, "A ticket the admin will delete");
        String adminToken = loginAsNewAdmin();

        mockMvc.perform(delete("/api/v1/tickets/" + ticketId).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNoContent());

        assertFalse(ticketRepository.existsById(ticketId));
    }

    @Test
    void aUsersListOnlyContainsTheirOwnTickets() throws Exception {
        String aliceToken = register();
        String bobToken = register();
        long aliceTicket = createTicket(aliceToken, "Alice's laptop will not start");
        long bobTicket = createTicket(bobToken, "Bob's monitor flickers");
        long aliceId = currentUserId(aliceToken);

        mockMvc.perform(get("/api/v1/tickets").param("size", "100")
                        .header(HttpHeaders.AUTHORIZATION, bearer(aliceToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[*].id", hasItem((int) aliceTicket)))
                .andExpect(jsonPath("$.items[*].id", not(hasItem((int) bobTicket))))
                .andExpect(jsonPath("$.items[*].createdById", everyItem(is((int) aliceId))));
    }

    // Registers a new account through the API (always a USER) and returns its token.
    private String register() throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Test User\", \"email\": \"" + freshEmail()
                                + "\", \"password\": \"" + PASSWORD + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    // Register only makes USERs, so the admin is saved straight into the
    // database (as the README's psql step does by hand), then logs in through
    // the API like anyone else.
    private String loginAsNewAdmin() throws Exception {
        String email = freshEmail();
        AppUser admin = new AppUser("Test Admin", email, "", UserRole.ADMIN, Instant.now());
        admin.setPasswordHash(passwordEncoder.encode(PASSWORD));
        appUserRepository.save(admin);

        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + email + "\", \"password\": \"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private long createTicket(String token, String title) throws Exception {
        String body = mockMvc.perform(post("/api/v1/tickets")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"" + title + "\", \"description\": \"Made by a test.\","
                                + " \"priority\": \"LOW\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long currentUserId(String token) throws Exception {
        String body = mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String freshEmail() {
        return "test-" + UUID.randomUUID() + "@example.com";
    }
}
