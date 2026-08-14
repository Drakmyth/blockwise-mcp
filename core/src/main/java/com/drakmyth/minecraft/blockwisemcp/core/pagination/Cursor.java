package com.drakmyth.minecraft.blockwisemcp.core.pagination;

import java.util.Objects;
import java.util.UUID;

record Cursor(int formatVersion, UUID generation, String queryIdentity, String position) {
    public Cursor {
        Objects.requireNonNull(generation, "generation");
        Objects.requireNonNull(queryIdentity, "queryIdentity");
        Objects.requireNonNull(position, "position");
    }
}
