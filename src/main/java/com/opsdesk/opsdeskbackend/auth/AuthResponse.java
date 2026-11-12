package com.opsdesk.opsdeskbackend.auth;

import com.opsdesk.opsdeskbackend.common.ApiDates;
import com.opsdesk.opsdeskbackend.security.IssuedToken;
import com.opsdesk.opsdeskbackend.user.AppUser;
import com.opsdesk.opsdeskbackend.user.UserResponse;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

// What register and login return: the token to send on later requests, when
// it stops working, and who it belongs to.
public record AuthResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED,
                description = "Send it as \"Authorization: Bearer <token>\" on every other request.",
                example = "eyJhbGciOiJIUzI1NiJ9.example.signature")
        String token,

        @Schema(requiredMode = RequiredMode.REQUIRED, format = "date-time",
                description = "After this, the token gets 401 and the person must log in again.",
                example = "2026-10-16T09:30:00.000Z")
        String expiresAt,

        @Schema(requiredMode = RequiredMode.REQUIRED)
        UserResponse user) {

    public static AuthResponse from(IssuedToken token, AppUser user) {
        return new AuthResponse(token.token(), ApiDates.format(token.expiresAt()), UserResponse.from(user));
    }
}
