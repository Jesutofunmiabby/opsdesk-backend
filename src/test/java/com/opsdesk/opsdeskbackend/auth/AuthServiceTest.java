package com.opsdesk.opsdeskbackend.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.opsdesk.opsdeskbackend.common.ConflictException;
import com.opsdesk.opsdeskbackend.common.UnauthenticatedException;
import com.opsdesk.opsdeskbackend.security.JwtService;
import com.opsdesk.opsdeskbackend.user.AppUser;
import com.opsdesk.opsdeskbackend.user.AppUserRepository;
import com.opsdesk.opsdeskbackend.user.UserRole;

// The repository is a mock; the password encoder and token service are real.
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String PASSWORD = "correct horse battery staple";

    @Mock
    private AppUserRepository appUserRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private AuthService authService;

    @BeforeEach
    void setUp() {
        JwtService jwtService = new JwtService("a-test-secret-that-is-long-enough-for-hs256", Duration.ofHours(1));
        authService = new AuthService(appUserRepository, passwordEncoder, jwtService);
    }

    @Test
    void registerMakesAUserWithAHashedPassword() {
        // Like the database, saving gives the user an id.
        when(appUserRepository.saveAndFlush(any(AppUser.class))).thenAnswer(call -> {
            AppUser user = call.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 5L);
            return user;
        });

        AuthResponse response = authService.register(
                new RegisterRequest("Amara Okafor", "Amara.Okafor@Example.com", PASSWORD));

        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).saveAndFlush(saved.capture());
        AppUser user = saved.getValue();
        assertEquals(UserRole.USER, user.getRole());
        assertEquals("amara.okafor@example.com", user.getEmail());
        assertNotEquals(PASSWORD, user.getPasswordHash());
        assertTrue(user.getPasswordHash().startsWith("$2"));
        assertTrue(passwordEncoder.matches(PASSWORD, user.getPasswordHash()));
        assertEquals(UserRole.USER, response.user().role());
    }

    @Test
    void registerWithATakenEmailIsAConflict() {
        when(appUserRepository.existsByEmail("amara.okafor@example.com")).thenReturn(true);

        ConflictException e = assertThrows(ConflictException.class, () -> authService.register(
                new RegisterRequest("Amara Okafor", "AMARA.OKAFOR@example.com", PASSWORD)));

        assertEquals("An account with this email address already exists.", e.getMessage());
        verify(appUserRepository, never()).saveAndFlush(any(AppUser.class));
    }

    @Test
    void loginWithTheRightPasswordGivesAToken() {
        AppUser user = userWithPassword(PASSWORD);
        when(appUserRepository.findByEmail("amara.okafor@example.com")).thenReturn(Optional.of(user));

        AuthResponse response = authService.login(new LoginRequest("Amara.Okafor@example.com", PASSWORD));

        assertEquals(3, response.token().split("\\.").length);
        assertEquals("amara.okafor@example.com", response.user().email());
    }

    @Test
    void aWrongPasswordAndAnUnknownEmailGiveTheSameError() {
        AppUser user = userWithPassword(PASSWORD);
        when(appUserRepository.findByEmail("amara.okafor@example.com")).thenReturn(Optional.of(user));
        when(appUserRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        UnauthenticatedException wrongPassword = assertThrows(UnauthenticatedException.class,
                () -> authService.login(new LoginRequest("amara.okafor@example.com", "wrong password")));
        UnauthenticatedException unknownEmail = assertThrows(UnauthenticatedException.class,
                () -> authService.login(new LoginRequest("nobody@example.com", "wrong password")));

        assertEquals("The email address or password is wrong.", wrongPassword.getMessage());
        assertEquals(wrongPassword.getMessage(), unknownEmail.getMessage());
    }

    @Test
    void aUserWithNoPasswordCannotLogIn() {
        AppUser seeded = new AppUser("Thabo Molefe", "thabo.molefe@example.com", "", UserRole.AGENT, Instant.now());
        when(appUserRepository.findByEmail("thabo.molefe@example.com")).thenReturn(Optional.of(seeded));

        assertThrows(UnauthenticatedException.class,
                () -> authService.login(new LoginRequest("thabo.molefe@example.com", "")));
    }

    private AppUser userWithPassword(String password) {
        AppUser user = new AppUser("Amara Okafor", "amara.okafor@example.com", "", UserRole.USER, Instant.now());
        user.setPasswordHash(passwordEncoder.encode(password));
        ReflectionTestUtils.setField(user, "id", 5L);
        return user;
    }
}
