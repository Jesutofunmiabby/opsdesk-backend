package com.opsdesk.opsdeskbackend.ticket;

// The body of PATCH /tickets/{id}/assignee: a user's id, or null to unassign.
public record AssignTicketRequest(Long assigneeId) {
}
