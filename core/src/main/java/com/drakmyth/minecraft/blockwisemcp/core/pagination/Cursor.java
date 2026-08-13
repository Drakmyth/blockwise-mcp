package com.drakmyth.minecraft.blockwisemcp.core.pagination;

import java.util.Objects;

record Cursor(int formatVersion, long generation, String queryIdentity, String position) {
    public Cursor {
        Objects.requireNonNull(queryIdentity, "queryIdentity");
        Objects.requireNonNull(position, "position");
    }
}
