package com.opsdesk.opsdeskbackend.common;

// The request clashes with data that is already there, such as an email that
// is taken. Becomes 409 CONFLICT.
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
