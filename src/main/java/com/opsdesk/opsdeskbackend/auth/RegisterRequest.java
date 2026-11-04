package com.opsdesk.opsdeskbackend.auth;

import com.opsdesk.opsdeskbackend.common.Text;
import com.opsdesk.opsdeskbackend.common.TextOnlyDeserializer;

import tools.jackson.databind.annotation.JsonDeserialize;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// There is no role field on purpose: register always makes a USER, and a
// "role" sent in the body is ignored like any other unknown field.
public record RegisterRequest(
        @NotBlank(message = "Enter your name.")
        @Size(max = 100, message = "The name must be 100 characters or fewer.")
        @JsonDeserialize(using = TextOnlyDeserializer.class)
        String name,

        @NotBlank(message = "Enter a valid email address.")
        @Email(message = "Enter a valid email address.")
        @Size(max = 254, message = "Enter a valid email address.")
        @JsonDeserialize(using = TextOnlyDeserializer.class)
        String email,

        // Not trimmed: spaces in a password are part of it.
        @NotNull(message = "The password must be at least 8 characters.")
        @Size(min = 8, message = "The password must be at least 8 characters.")
        @Size(max = 128, message = "The password must be 128 characters or fewer.")
        @JsonDeserialize(using = TextOnlyDeserializer.class)
        String password) {

    public RegisterRequest {
        name = Text.trim(name);
        email = Text.trim(email);
    }

    // Keeps the password out of logs and error messages.
    @Override
    public String toString() {
        return "RegisterRequest[name=" + name + ", email=" + email + ", password=****]";
    }
}
