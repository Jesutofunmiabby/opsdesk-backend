package com.opsdesk.opsdeskbackend.user;

import com.opsdesk.opsdeskbackend.common.ApiDates;

// What the API shows of a user. A password, or anything worked out from one,
// must never be added here.
public record UserResponse(
        long id,
        String name,
        String email,
        String jobTitle,
        UserRole role,
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
