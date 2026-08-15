package com.drakmyth.minecraft.blockwisemcp.neoforge;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(NeoForgeBlockwiseMcp.MOD_ID)
public final class NeoForgeBlockwiseMcp {
    public static final String MOD_ID = "blockwisemcp";

    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean initialized;
    private static NeoForgeMcpLifecycle mcpLifecycle;

    public NeoForgeBlockwiseMcp(ModContainer container) {
        var config = new NeoForgeConfig();
        container.registerConfig(ModConfig.Type.COMMON, config.spec());
        mcpLifecycle = new NeoForgeMcpLifecycle(LOGGER, container.getModInfo().getVersion().toString(), config);
        NeoForge.EVENT_BUS.addListener(mcpLifecycle::serverStarted);
        NeoForge.EVENT_BUS.addListener(mcpLifecycle::tagsUpdated);
        NeoForge.EVENT_BUS.addListener(mcpLifecycle::serverStopping);
        initialized = true;
        LOGGER.info("Blockwise MCP initialized");
    }

    static boolean isInitialized() {
        return initialized;
    }

    static boolean isMcpRunning() {
        return mcpLifecycle != null && mcpLifecycle.isRunning();
    }
}
