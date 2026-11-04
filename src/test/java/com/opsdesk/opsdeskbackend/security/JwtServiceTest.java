package com.opsdesk.opsdeskbackend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.opsdesk.opsdeskbackend.user.AppUser;
import com.opsdesk.opsdeskbackend.user.UserRole;

import io.jsonwebtoken.security.WeakKeyException;

class JwtServiceTest {

    private static final String SECRET = "a-test-secret-that-is-long-enough-for-hs256";

    private final JwtService jwtService = new JwtService(SECRET, Duration.ofHours(1));

    @Test
    void aTokenItIssuedReadsBackAsTheSameUserAndRole() {
        IssuedToken issued = jwtService.issue(user(5L, UserRole.MANAGER));

        assertEquals(Optional.of(new AuthUser(5L, UserRole.MANAGER)), jwtService.read(issued.token()));
    }

    @Test
    void aChangedTokenIsRejected() {
        String token = jwtService.issue(user(5L, UserRole.USER)).token();
        String[] parts = token.split("\\.");
        // Swap the payload for one that claims ADMIN, keeping the old signature.
        String adminPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"5\",\"role\":\"ADMIN\"}".getBytes());

        assertTrue(jwtService.read(parts[0] + "." + adminPayload + "." + parts[2]).isEmpty());
    }

    @Test
    void aTokenSignedWithAnotherSecretIsRejected() {
        JwtService other = new JwtService("another-secret-that-is-also-long-enough-123", Duration.ofHours(1));
        String token = other.issue(user(5L, UserRole.USER)).token();

        assertTrue(jwtService.read(token).isEmpty());
    }

    @Test
    void anExpiredTokenIsRejected() {
        JwtService alreadyExpired = new JwtService(SECRET, Duration.ofSeconds(-1));
        String token = alreadyExpired.issue(user(5L, UserRole.USER)).token();

        assertTrue(jwtService.read(token).isEmpty());
    }

    @Test
    void textThatIsNotATokenIsRejected() {
        assertTrue(jwtService.read("not-a-token").isEmpty());
        assertTrue(jwtService.read("").isEmpty());
    }

    @Test
    void aSecretShorterThan32BytesStopsTheAppStarting() {
        assertThrows(WeakKeyException.class, () -> new JwtService("too-short", Duration.ofHours(1)));
    }

    private static AppUser user(long id, UserRole role) {
        AppUser user = mock(AppUser.class);
        when(user.getId()).thenReturn(id);
        when(user.getRole()).thenReturn(role);
        return user;
    }
}
