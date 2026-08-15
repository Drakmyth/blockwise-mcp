package com.drakmyth.minecraft.blockwisemcp.mcp.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ToolLoggingTest {
    @Test
    void marksOmittedFilters() {
        assertEquals("<omitted>", ToolLogging.filter(null));
    }

    @Test
    void escapesFilterControlCharactersAndQuotes() {
        assertEquals("a\\\\b\\\"c\\n\\r\\t\\u0001", ToolLogging.filter("a\\b\"c\n\r\t\u0001"));
    }

    @Test
    void truncatesLongFilters() {
        assertEquals("a".repeat(128) + "...", ToolLogging.filter("a".repeat(129)));
    }
}
