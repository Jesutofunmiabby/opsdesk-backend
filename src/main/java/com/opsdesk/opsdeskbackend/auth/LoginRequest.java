package com.opsdesk.opsdeskbackend.auth;

import com.opsdesk.opsdeskbackend.common.Text;
import com.opsdesk.opsdeskbackend.common.TextOnlyDeserializer;

import tools.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record LoginRequest(
        @Schema(requiredMode = RequiredMode.REQUIRED, example = "amara.okafor@example.com")
        @NotBlank(message = "Enter your email address.")
        @JsonDeserialize(using = TextOnlyDeserializer.class)
        String email,

        @Schema(requiredMode = RequiredMode.REQUIRED, format = "password", example = "correct horse battery staple")
        @NotEmpty(message = "Enter your password.")
        @JsonDeserialize(using = TextOnlyDeserializer.class)
        String password) {

    public LoginRequest {
        email = Text.trim(email);
    }

    // Keeps the password out of logs and error messages.
    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", password=****]";
    }
}
