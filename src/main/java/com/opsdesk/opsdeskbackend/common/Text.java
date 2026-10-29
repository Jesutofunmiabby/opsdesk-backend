package com.opsdesk.opsdeskbackend.common;

import java.util.regex.Pattern;

public final class Text {

    // The characters JavaScript's trim() removes, which is what the frontend
    // checks with: spaces, tabs, line breaks, the byte order mark and every
    // Unicode space, such as the non-breaking space pasted in from documents.
    // Java's strip() keeps some of these, so the two would disagree on
    // whether a title is long enough.
    private static final Pattern SPACE_AT_ENDS =
            Pattern.compile("^[\\s\\p{Z}\\uFEFF]+|[\\s\\p{Z}\\uFEFF]+$");

    private Text() {
    }

    public static String trim(String text) {
        return text == null ? null : SPACE_AT_ENDS.matcher(text).replaceAll("");
    }
}
