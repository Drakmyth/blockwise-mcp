package com.drakmyth.minecraft.blockwisemcp.core.pagination;

import static com.drakmyth.minecraft.blockwisemcp.core.pagination.InvalidCursorException.Reason.MALFORMED;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Base64;

public final class CursorCodec {
    public String encode(Cursor cursor) {
        try {
            var bytes = new ByteArrayOutputStream();
            try (var output = new DataOutputStream(bytes)) {
                output.writeInt(cursor.formatVersion());
                output.writeLong(cursor.generation());
                output.writeUTF(cursor.queryIdentity());
                output.writeUTF(cursor.position());
            }
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not encode cursor", exception);
        }
    }

    public Cursor decode(String encoded) {
        try {
            var bytes = Base64.getUrlDecoder().decode(encoded);
            try (var input = new DataInputStream(new ByteArrayInputStream(bytes))) {
                var cursor = new Cursor(input.readInt(), input.readLong(), input.readUTF(), input.readUTF());
                if (input.available() != 0) {
                    throw malformed(null);
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

    private static InvalidCursorException malformed(Exception cause) {
        var exception = new InvalidCursorException(MALFORMED, "Cursor is malformed");
        if (cause != null) {
            exception.initCause(cause);
        }
        return exception;
    }
}
