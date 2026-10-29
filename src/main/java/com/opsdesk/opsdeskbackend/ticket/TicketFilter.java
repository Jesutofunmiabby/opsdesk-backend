package com.opsdesk.opsdeskbackend.ticket;

import org.springframework.data.jpa.domain.Specification;

// The filters for GET /api/v1/tickets. Each one is optional (null means no
// filter), and they combine: status=OPEN&priority=HIGH means open and high.
public record TicketFilter(TicketStatus status, TicketPriority priority, String search) {

    // The contract ignores spaces at either end, and an empty search means no
    // search.
    public TicketFilter {
        search = search == null || search.isBlank() ? null : search.strip();
    }

    // Builds the WHERE clause from only the filters that were sent.
    Specification<Ticket> toSpecification() {
        Specification<Ticket> spec = Specification.unrestricted();
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (priority != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("priority"), priority));
        }
        if (search != null) {
            // Title contains the text, ignoring capitals. % and _ are escaped so
            // they are searched for as themselves, not used as wildcards.
            String pattern = "%" + search.toLowerCase()
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("title")), pattern, '\\'));
        }
        return spec;
    }
}
