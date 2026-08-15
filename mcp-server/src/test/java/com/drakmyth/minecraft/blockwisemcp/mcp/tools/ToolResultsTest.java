package com.drakmyth.minecraft.blockwisemcp.mcp.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.drakmyth.minecraft.blockwisemcp.core.pagination.InvalidCursorException;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;

class ToolResultsTest {
    @Test
    void classifiesCursorFailures() {
        assertFailure(
                new InvalidCursorException(InvalidCursorException.Reason.MALFORMED, "ignored"),
                ToolError.CURSOR_MALFORMED);
        assertFailure(
                new InvalidCursorException(InvalidCursorException.Reason.UNSUPPORTED_FORMAT, "ignored"),
                ToolError.CURSOR_FORMAT_UNSUPPORTED);
        assertFailure(
                new InvalidCursorException(InvalidCursorException.Reason.STALE, "ignored"),
                ToolError.CURSOR_STALE);
        assertFailure(
                new InvalidCursorException(InvalidCursorException.Reason.QUERY_MISMATCH, "ignored"),
                ToolError.CURSOR_QUERY_MISMATCH);
    }

    @Test
    void classifiesExecutionFailures() {
        assertFailure(new TimeoutException("ignored"), ToolError.EXECUTION_TIMEOUT);
        assertFailure(new InterruptedException("ignored"), ToolError.EXECUTION_INTERRUPTED);
    }

    @Test
    void sanitizesUnexpectedFailures() {
        assertFailure(new IllegalStateException("sensitive detail"), ToolError.INTERNAL_ERROR);
    }

    private static void assertFailure(Exception exception, ToolError expected) {
        var result = ToolResults.failure(exception);

        assertTrue(result.isError());
        assertNull(result.structuredContent());
        assertEquals(expected.message(), ((TextContent) result.content().getFirst()).text());
    }
}
