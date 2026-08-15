package com.drakmyth.minecraft.blockwisemcp.fabric;

import com.drakmyth.minecraft.blockwisemcp.core.mods.LoadedMod;
import com.drakmyth.minecraft.blockwisemcp.core.mods.LoadedModSource;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;

public final class FabricLoadedModSource implements LoadedModSource {
    @Override
    public List<LoadedMod> getLoadedMods() {
        return FabricLoader.getInstance().getAllMods().stream()
                .map(container -> new LoadedMod(
                        container.getMetadata().getId(),
                        container.getMetadata().getName(),
                        container.getMetadata().getVersion().getFriendlyString()))
                .toList();
    }
}
