package com.opsdesk.opsdeskbackend.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.opsdesk.opsdeskbackend.common.ErrorResponse;
import com.opsdesk.opsdeskbackend.security.AuthUser;
import com.opsdesk.opsdeskbackend.user.UserResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

// register and login are open to everyone (see SecurityConfig); me needs a
// valid token. The empty @SecurityRequirements tells the API documentation
// that register and login need no token.
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @SecurityRequirements
    @Operation(summary = "Register",
            description = "Creates an account and logs it in. The role is always USER: a role in the body is ignored.")
    @ApiResponse(responseCode = "201", description = "Created and logged in.")
    @ApiResponse(responseCode = "400", description = "A field breaks its rule, or the body is not a JSON object.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "An account with this email address already exists.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Log in")
    @ApiResponse(responseCode = "200", description = "Logged in.")
    @ApiResponse(responseCode = "400", description = "email or password is missing.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "The email is not registered or the password is wrong "
            + "(the same message for both).",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    @Operation(summary = "The logged-in user")
    @ApiResponse(responseCode = "200", description = "The user the token belongs to.")
    public UserResponse me(@AuthenticationPrincipal AuthUser authUser) {
        return UserResponse.from(authService.findCurrentUser(authUser));
    }
}
