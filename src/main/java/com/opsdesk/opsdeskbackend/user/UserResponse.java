package com.opsdesk.opsdeskbackend.user;

import com.opsdesk.opsdeskbackend.common.ApiDates;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

// What the API shows of a user. A password, or anything worked out from one,
// must never be added here.
@Schema(name = "User")
public record UserResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED, example = "7")
        long id,

        @Schema(requiredMode = RequiredMode.REQUIRED, minLength = 1, maxLength = 100, example = "Amara Okafor")
        String name,

        @Schema(requiredMode = RequiredMode.REQUIRED, format = "email",
                description = "Unique across all users, ignoring capitals.", example = "amara.okafor@example.com")
        String email,

        @Schema(requiredMode = RequiredMode.REQUIRED, description = "May be empty.", example = "IT Support Lead")
        String jobTitle,

        @Schema(requiredMode = RequiredMode.REQUIRED, description = "A new user from register is always USER.")
        UserRole role,

        @Schema(requiredMode = RequiredMode.REQUIRED, format = "date-time",
                description = "When the account was created.", example = "2026-09-01T09:00:00.000Z")
        String createdAt) {

    public static UserResponse from(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getJobTitle(),
                user.getRole(),
                ApiDates.format(user.getCreatedAt()));
    }
}
