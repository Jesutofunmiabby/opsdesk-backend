package com.opsdesk.opsdeskbackend.ticket;

import com.opsdesk.opsdeskbackend.common.NotFoundException;

public class TicketNotFoundException extends NotFoundException {

    public TicketNotFoundException(String id) {
        super("ticket", id);
    }
}
