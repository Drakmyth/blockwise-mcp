package com.drakmyth.minecraft.blockwisemcp.core.pagination;

import java.util.List;
import java.util.Objects;

public record Page<T>(List<T> items, String nextCursor) {
    public Page {
        items = List.copyOf(Objects.requireNonNull(items, "items"));
    }
}
