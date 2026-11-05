package com.opsdesk.opsdeskbackend.ticket;

import org.springframework.data.jpa.domain.Specification;

import com.opsdesk.opsdeskbackend.common.Text;

// The filters for GET /api/v1/tickets. Each one is optional (null means no
// filter), and they combine: status=OPEN&priority=HIGH means open and high.
public record TicketFilter(TicketStatus status, TicketPriority priority, Long assigneeId, String q) {

    // The contract ignores spaces at either end, and an empty q means no
    // search.
    public TicketFilter {
        q = Text.trim(q);
        if (q != null && q.isEmpty()) {
            q = null;
        }
    }

    // Builds the WHERE clause from only the filters that were sent. Each
    // filter is a Specification: one condition that can be joined to others
    // with and().
    Specification<Ticket> toSpecification() {
        Specification<Ticket> spec = Specification.unrestricted();
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (priority != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("priority"), priority));
        }
        if (assigneeId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("assignee").get("id"), assigneeId));
        }
        if (q != null) {
            // Title contains the text, ignoring capitals. % and _ are escaped so
            // they are searched for as themselves, not used as wildcards.
            String pattern = "%" + q.toLowerCase()
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("title")), pattern, '\\'));
        }
        return spec;
    }
}
