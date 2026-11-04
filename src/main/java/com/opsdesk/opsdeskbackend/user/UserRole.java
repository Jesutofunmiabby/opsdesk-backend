package com.opsdesk.opsdeskbackend.user;

// What a user is allowed to do is set by their role: see the permission table
// in the README. A new account from register is always a USER.
public enum UserRole {
    ADMIN,
    MANAGER,
    AGENT,
    USER
}
