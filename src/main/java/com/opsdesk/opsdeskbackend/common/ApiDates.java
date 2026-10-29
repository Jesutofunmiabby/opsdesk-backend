package com.opsdesk.opsdeskbackend.common;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

// The contract's format for a moment in time: UTC with milliseconds, e.g.
// 2026-10-15T09:30:00.000Z.
public final class ApiDates {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private ApiDates() {
    }

    public static String format(Instant instant) {
        return FORMAT.format(instant);
    }
}
