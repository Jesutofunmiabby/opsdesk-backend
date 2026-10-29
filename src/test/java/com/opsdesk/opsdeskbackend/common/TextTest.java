package com.opsdesk.opsdeskbackend.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class TextTest {

    @Test
    void trimsTheSameCharactersAsJavaScript() {
        // A non-breaking space, an ideographic space, a tab, a line break
        // and a byte order mark, as JavaScript's "...".trim() removes them.
        assertEquals("ab", Text.trim(" 　\t ab\n﻿"));
    }

    @Test
    void keepsSpacesInTheMiddleAndLeavesNullAlone() {
        assertEquals("Printer  jam", Text.trim("  Printer  jam  "));
        assertNull(Text.trim(null));
    }
}
