package com.opsdesk.opsdeskbackend.security;

import java.time.Instant;

// A new login token and the moment it stops working.
public record IssuedToken(String token, Instant expiresAt) {
}
