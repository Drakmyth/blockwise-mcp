package com.drakmyth.minecraft.blockwisemcp.core.pagination;

public final class InvalidCursorException extends IllegalArgumentException {
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
