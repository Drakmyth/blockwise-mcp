package com.drakmyth.minecraft.blockwisemcp.mcp.tools;

import com.drakmyth.minecraft.blockwisemcp.core.pagination.InvalidCursorException;
import java.util.concurrent.TimeoutException;

enum ToolError {
    CURSOR_MALFORMED("The cursor is invalid. Discard it and restart pagination without a cursor."),
    CURSOR_FORMAT_UNSUPPORTED(
            "The cursor format is unsupported. Discard it and restart pagination without a cursor."),
    CURSOR_STALE("Runtime data changed after the cursor was issued. Repeat the same query without a cursor."),
    CURSOR_QUERY_MISMATCH(
            "The cursor belongs to a different query. Restore the original filters or restart without a cursor."),
    EXECUTION_TIMEOUT("The request did not complete in time. Retry later, but do not retry repeatedly."),
    EXECUTION_INTERRUPTED("The request was interrupted. Retry after the active Minecraft runtime is ready."),
    INTERNAL_ERROR(
            "Blockwise MCP could not complete the request. Do not retry unchanged; consult the server logs or report the failure.");

    private final String guidance;

    ToolError(String guidance) {
        this.guidance = guidance;
    }

    static ToolError from(Exception exception) {
        if (exception instanceof InvalidCursorException invalidCursor) {
            return switch (invalidCursor.reason()) {
                case MALFORMED -> CURSOR_MALFORMED;
                case UNSUPPORTED_FORMAT -> CURSOR_FORMAT_UNSUPPORTED;
                case STALE -> CURSOR_STALE;
                case QUERY_MISMATCH -> CURSOR_QUERY_MISMATCH;
            };
        }
        if (exception instanceof TimeoutException) {
            return EXECUTION_TIMEOUT;
        }
        if (exception instanceof InterruptedException) {
            return EXECUTION_INTERRUPTED;
        }
        return INTERNAL_ERROR;
    }

    String message() {
        return name() + ": " + guidance;
    }
}
