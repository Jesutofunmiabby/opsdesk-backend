package com.opsdesk.opsdeskbackend.security;

import com.opsdesk.opsdeskbackend.user.UserRole;

// The logged-in user, as read from their token. Controllers get it with
// @AuthenticationPrincipal. The role comes from the token the server signed,
// never from anything else in the request.
public record AuthUser(long id, UserRole role) {
}
