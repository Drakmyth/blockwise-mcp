package com.drakmyth.minecraft.blockwisemcp.core.pagination;

import java.util.List;
import java.util.Objects;

/**
 * An immutable page of results.
 *
 * @param items results in service-defined stable order
 * @param nextCursor opaque continuation cursor, or {@code null} for the final page
 */
public record Page<T>(List<T> items, String nextCursor) {
    public Page {
        items = List.copyOf(Objects.requireNonNull(items, "items"));
    }
}
