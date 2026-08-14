package com.drakmyth.minecraft.blockwisemcp.neoforge;

import com.drakmyth.minecraft.blockwisemcp.core.mods.LoadedMod;
import com.drakmyth.minecraft.blockwisemcp.core.mods.LoadedModSource;
import java.util.List;
import net.neoforged.fml.ModList;

public final class NeoForgeLoadedModSource implements LoadedModSource {
    @Override
    public List<LoadedMod> getLoadedMods() {
        return ModList.get().getMods().stream()
                .map(mod -> new LoadedMod(
                        mod.getModId(),
                        mod.getDisplayName(),
                        mod.getVersion().toString()))
                .toList();
    }
}
