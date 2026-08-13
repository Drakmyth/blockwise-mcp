package com.drakmyth.minecraft.blockwisemcp.core;

import java.util.Objects;

public record LoadedMod(String id, String displayName, String version) {
    public LoadedMod {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(version, "version");
    }
}
