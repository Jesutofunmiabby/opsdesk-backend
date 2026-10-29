package com.opsdesk.opsdeskbackend.common;

// There is nothing at this id. Becomes 404 NOT_FOUND with the contract's
// message, e.g. There is no ticket with the id "99".
public class NotFoundException extends RuntimeException {

    public NotFoundException(String thing, String id) {
        super("There is no " + thing + " with the id \"" + id + "\".");
    }
}
