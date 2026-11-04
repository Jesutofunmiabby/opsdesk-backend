package com.opsdesk.opsdeskbackend.auth;

import com.opsdesk.opsdeskbackend.common.Text;
import com.opsdesk.opsdeskbackend.common.TextOnlyDeserializer;

import tools.jackson.databind.annotation.JsonDeserialize;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record LoginRequest(
        @NotBlank(message = "Enter your email address.")
        @JsonDeserialize(using = TextOnlyDeserializer.class)
        String email,

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
