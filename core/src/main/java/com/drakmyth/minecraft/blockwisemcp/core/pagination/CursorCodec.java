package com.drakmyth.minecraft.blockwisemcp.core.pagination;

import static com.drakmyth.minecraft.blockwisemcp.core.pagination.InvalidCursorException.Reason.MALFORMED;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

/** Encodes and validates opaque, URL-safe pagination cursors. */
public final class CursorCodec {
    /** Creates an opaque cursor from service-owned pagination state. */
    public String encode(int formatVersion, UUID generation, String queryIdentity, String position) {
        var cursor = new Cursor(formatVersion, generation, queryIdentity, position);
        try {
            var bytes = new ByteArrayOutputStream();
            try (var output = new DataOutputStream(bytes)) {
                output.writeInt(cursor.formatVersion());
                output.writeLong(cursor.generation().getMostSignificantBits());
                output.writeLong(cursor.generation().getLeastSignificantBits());
                output.writeUTF(cursor.queryIdentity());
                output.writeUTF(cursor.position());
            }
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not encode cursor", exception);
        }
    }

    /**
     * Validates a cursor against the current query and returns its internal position.
     *
     * @throws InvalidCursorException if the cursor is malformed, unsupported, stale, or mismatched
     */
    public String decodePosition(
            String encoded,
            int expectedFormatVersion,
            UUID expectedGeneration,
            String expectedQueryIdentity) {
        var cursor = decode(encoded, expectedFormatVersion);
        if (!cursor.generation().equals(expectedGeneration)) {
            throw new InvalidCursorException(InvalidCursorException.Reason.STALE, "Cursor is stale");
        }
        if (!cursor.queryIdentity().equals(expectedQueryIdentity)) {
            throw new InvalidCursorException(
                    InvalidCursorException.Reason.QUERY_MISMATCH,
                    "Cursor does not match the query");
        }
        return cursor.position();
    }

    private Cursor decode(String encoded, int expectedFormatVersion) {
        try {
            var bytes = Base64.getUrlDecoder().decode(encoded);
            try (var input = new DataInputStream(new ByteArrayInputStream(bytes))) {
                var formatVersion = input.readInt();
                if (formatVersion != expectedFormatVersion) {
                    throw new InvalidCursorException(
                            InvalidCursorException.Reason.UNSUPPORTED_FORMAT,
                            "Cursor format is unsupported");
                }
                var generation = new UUID(input.readLong(), input.readLong());
                var cursor = new Cursor(formatVersion, generation, input.readUTF(), input.readUTF());
                if (input.available() != 0) {
                    throw malformed();
                }
                return cursor;
            }
        } catch (IllegalArgumentException | IOException exception) {
            if (exception instanceof InvalidCursorException invalidCursorException) {
                throw invalidCursorException;
            }
            throw malformed(exception);
        }
    }

    private static InvalidCursorException malformed() {
        return new InvalidCursorException(MALFORMED, "Cursor is malformed");
    }

    private static InvalidCursorException malformed(Exception cause) {
        var exception = malformed();
        exception.initCause(cause);
        return exception;
    }
}
