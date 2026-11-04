package com.opsdesk.opsdeskbackend.common;

// The caller is not logged in, or their email or password is wrong.
// Becomes 401 UNAUTHENTICATED.
public class UnauthenticatedException extends RuntimeException {

    public UnauthenticatedException(String message) {
        super(message);
    }
}
