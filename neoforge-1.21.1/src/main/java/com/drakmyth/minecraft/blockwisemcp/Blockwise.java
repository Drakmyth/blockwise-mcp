package com.drakmyth.minecraft.blockwisemcp;

import com.drakmyth.minecraft.blockwisemcp.neoforge.BlockwiseConfig;
import com.drakmyth.minecraft.blockwisemcp.neoforge.NeoForgeMcpLifecycle;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(Blockwise.MOD_ID)
public final class Blockwise {
    public static final String MOD_ID = "blockwisemcp";

    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean initialized;
    private static NeoForgeMcpLifecycle mcpLifecycle;

    public Blockwise(ModContainer container) {
        var config = new BlockwiseConfig();
        container.registerConfig(ModConfig.Type.COMMON, config.spec());
        mcpLifecycle = new NeoForgeMcpLifecycle(LOGGER, container.getModInfo().getVersion().toString(), config);
        NeoForge.EVENT_BUS.addListener(mcpLifecycle::serverStarted);
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
