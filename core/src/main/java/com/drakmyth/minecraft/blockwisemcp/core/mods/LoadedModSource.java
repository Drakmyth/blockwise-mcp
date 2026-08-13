package com.drakmyth.minecraft.blockwisemcp.core.mods;

import java.util.List;

@FunctionalInterface
public interface LoadedModSource {
    List<LoadedMod> getLoadedMods();
}
