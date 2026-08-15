package com.drakmyth.minecraft.blockwisemcp.fabric;

import java.nio.file.Path;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Registers Blockwise MCP with the Fabric runtime. */
public final class BlockwiseFabric implements ModInitializer {
    public static final String MOD_ID = "blockwisemcp";
    private static final Logger LOGGER = LoggerFactory.getLogger("Blockwise MCP");
    private static FabricMcpLifecycle lifecycle;

    @Override
    public void onInitialize() {
        var loader = FabricLoader.getInstance();
        var version = loader.getModContainer(MOD_ID)
                .orElseThrow(() -> new IllegalStateException("Blockwise MCP metadata is unavailable"))
                .getMetadata()
                .getVersion()
                .getFriendlyString();
        Path configPath = loader.getConfigDir().resolve("blockwisemcp.json");
        FabricConfig config;
        try {
            config = FabricConfig.load(configPath);
        } catch (Exception exception) {
            LOGGER.error("Blockwise MCP configuration failed to load; MCP is disabled for this game session", exception);
            config = FabricConfig.disabled();
        }

        lifecycle = new FabricMcpLifecycle(LOGGER, version, config);
        ServerLifecycleEvents.SERVER_STARTED.register(lifecycle::serverStarted);
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register(
                (server, resourceManager, successful) -> lifecycle.dataPackReloaded(successful));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> lifecycle.serverStopping());
    }

    static boolean isMcpRunning() {
        return lifecycle != null && lifecycle.isRunning();
    }
}
