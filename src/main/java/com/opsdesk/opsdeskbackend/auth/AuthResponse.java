package com.opsdesk.opsdeskbackend.auth;

import com.opsdesk.opsdeskbackend.common.ApiDates;
import com.opsdesk.opsdeskbackend.security.IssuedToken;
import com.opsdesk.opsdeskbackend.user.AppUser;
import com.opsdesk.opsdeskbackend.user.UserResponse;

// What register and login return: the token to send on later requests, when
// it stops working, and who it belongs to.
public record AuthResponse(
        String token,
        String expiresAt,
        UserResponse user) {

    public static AuthResponse from(IssuedToken token, AppUser user) {
        return new AuthResponse(token.token(), ApiDates.format(token.expiresAt()), UserResponse.from(user));
    }
}
