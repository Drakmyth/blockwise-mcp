package com.drakmyth.minecraft.blockwisemcp.core;

import java.util.List;

@FunctionalInterface
public interface LoadedModSource {
    List<LoadedMod> getLoadedMods();
}
