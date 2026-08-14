package com.drakmyth.minecraft.blockwisemcp.core.pagination;

import static com.drakmyth.minecraft.blockwisemcp.core.pagination.InvalidCursorException.Reason.MALFORMED;
import static com.drakmyth.minecraft.blockwisemcp.core.pagination.InvalidCursorException.Reason.QUERY_MISMATCH;
import static com.drakmyth.minecraft.blockwisemcp.core.pagination.InvalidCursorException.Reason.STALE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class CursorCodecTest {
    private static final int FORMAT_VERSION = 1;
    private static final UUID GENERATION = UUID.fromString("3f747dc7-1945-4e9f-96b6-a2543bc88c98");
    private static final UUID OTHER_GENERATION = UUID.fromString("c5667681-59c4-4508-b1b6-ff55e5e01b2c");

    private final CursorCodec codec = new CursorCodec();

    @Test
    void roundTripsPosition() {
        var cursor = codec.encode(FORMAT_VERSION, GENERATION, "query", "position");

        assertEquals("position", codec.decodePosition(cursor, FORMAT_VERSION, GENERATION, "query"));
    }

    @Test
    void rejectsStaleMismatchedAndMalformedCursors() {
        var cursor = codec.encode(FORMAT_VERSION, GENERATION, "query", "position");

        assertReason(STALE, () -> codec.decodePosition(cursor, FORMAT_VERSION, OTHER_GENERATION, "query"));
        assertReason(QUERY_MISMATCH, () -> codec.decodePosition(cursor, FORMAT_VERSION, GENERATION, "other"));
        assertReason(MALFORMED, () -> codec.decodePosition("not base64", FORMAT_VERSION, GENERATION, "query"));
    }

    private static void assertReason(InvalidCursorException.Reason reason, Runnable action) {
        var exception = assertThrows(InvalidCursorException.class, action::run);
        assertEquals(reason, exception.reason());
    }
}
