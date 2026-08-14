package com.drakmyth.minecraft.blockwisemcp.core.mods;

import java.util.List;

/** Supplies metadata for mods active in the current runtime. */
@FunctionalInterface
public interface LoadedModSource {
    /**
     * Returns an immutable snapshot in loader-provided order.
     *
     * @return active mod metadata
     */
    List<LoadedMod> getLoadedMods();
}
