package com.opsdesk.opsdeskbackend.ticket;

public class TicketNotFoundException extends RuntimeException {

    public TicketNotFoundException(String id) {
        super("There is no ticket with the id \"" + id + "\".");
    }
}
