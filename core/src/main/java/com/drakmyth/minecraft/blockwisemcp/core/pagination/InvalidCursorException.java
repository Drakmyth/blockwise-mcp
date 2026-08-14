package com.drakmyth.minecraft.blockwisemcp.core.pagination;

/** Indicates that an opaque pagination cursor cannot continue the requested query. */
public final class InvalidCursorException extends IllegalArgumentException {
    /** Machine-readable cursor failure categories. */
    public enum Reason {
        MALFORMED,
        UNSUPPORTED_FORMAT,
        QUERY_MISMATCH,
        STALE
    }

    private final Reason reason;

    public InvalidCursorException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
