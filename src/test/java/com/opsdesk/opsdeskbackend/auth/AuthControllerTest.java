package com.opsdesk.opsdeskbackend.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.opsdesk.opsdeskbackend.common.UnauthenticatedException;
import com.opsdesk.opsdeskbackend.security.AuthUser;
import com.opsdesk.opsdeskbackend.security.JsonAuthenticationEntryPoint;
import com.opsdesk.opsdeskbackend.security.JwtService;
import com.opsdesk.opsdeskbackend.security.SecurityConfig;
import com.opsdesk.opsdeskbackend.user.AppUser;
import com.opsdesk.opsdeskbackend.user.UserRole;

// Runs the real security filter chain and token checks in front of
// AuthController. AuthService is a mock.
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtService.class, JsonAuthenticationEntryPoint.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AuthService authService;

    @Test
    void registerNeedsNoTokenAndAnswers201() throws Exception {
        AuthResponse response = new AuthResponse("a.b.c", "2026-11-05T09:30:00.000Z", null);
        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Amara Okafor\", \"email\": \"amara@example.com\","
                                + " \"password\": \"correct horse battery staple\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("a.b.c"));
    }

    @Test
    void registerWithBadFieldsGivesTheContractMessages() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \" \", \"email\": \"not-an-email\", \"password\": \"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.name").value("Enter your name."))
                .andExpect(jsonPath("$.fields.email").value("Enter a valid email address."))
                .andExpect(jsonPath("$.fields.password").value("The password must be at least 8 characters."));
    }

    @Test
    void loginWithMissingFieldsGivesTheContractMessages() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.email").value("Enter your email address."))
                .andExpect(jsonPath("$.fields.password").value("Enter your password."));
    }

    @Test
    void aFailedLoginIs401InTheErrorShape() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new UnauthenticatedException("The email address or password is wrong."));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"amara@example.com\", \"password\": \"wrong password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.message").value("The email address or password is wrong."));
    }

    @Test
    void meWithoutATokenIs401InTheErrorShape() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.message").value("Log in to continue."));
    }

    @Test
    void meWithABrokenTokenIs401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer not-a-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meWithAValidTokenReturnsTheUserWithoutThePasswordHash() throws Exception {
        AppUser user = new AppUser("Amara Okafor", "amara@example.com", "", UserRole.USER,
                Instant.parse("2026-11-04T09:30:00Z"));
        user.setPasswordHash("$2a$10$abcdefghijklmnopqrstuuabcdefghijklmnopqrstuvwxyz12345");
        ReflectionTestUtils.setField(user, "id", 5L);
        when(authService.findCurrentUser(new AuthUser(5L, UserRole.USER))).thenReturn(user);

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + tokenFor(5L, UserRole.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("amara@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    private String tokenFor(long id, UserRole role) {
        AppUser user = mock(AppUser.class);
        when(user.getId()).thenReturn(id);
        when(user.getRole()).thenReturn(role);
        return jwtService.issue(user).token();
    }
}
