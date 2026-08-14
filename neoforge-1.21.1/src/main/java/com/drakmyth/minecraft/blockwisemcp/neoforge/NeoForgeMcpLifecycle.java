package com.drakmyth.minecraft.blockwisemcp.neoforge;

import com.drakmyth.minecraft.blockwisemcp.core.mods.ModService;
import com.drakmyth.minecraft.blockwisemcp.mcp.BlockwiseMcpServer;
import com.drakmyth.minecraft.blockwisemcp.mcp.tools.ListLoadedModsTool;
import java.util.List;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;

/** Composes and owns the MCP endpoint for each active Minecraft server. */
public final class NeoForgeMcpLifecycle {
    private static final long NON_GENERATIONAL_LOADED_MOD_DATA = 0;

    private final Logger logger;
    private final String version;
    private final BlockwiseConfig config;
    private BlockwiseMcpServer mcpServer;

    public NeoForgeMcpLifecycle(Logger logger, String version, BlockwiseConfig config) {
        this.logger = logger;
        this.version = version;
        this.config = config;
    }

    public void serverStarted(ServerStartedEvent event) {
        if (!config.enabled()) {
            logger.info("Blockwise MCP endpoint is disabled");
            return;
        }

        var timeout = config.dispatchTimeout();
        var service = new ModService(new NeoForgeLoadedModSource(), NON_GENERATIONAL_LOADED_MOD_DATA);
        var executor = new MinecraftServerToolExecutor(event.getServer(), timeout);
        var tools = List.of(ListLoadedModsTool.create(service, executor));
        try {
            mcpServer = BlockwiseMcpServer.start(config.port(), timeout, version, tools);
            logger.info("Blockwise MCP endpoint started at http://127.0.0.1:{}/mcp", mcpServer.port());
        } catch (Exception | LinkageError exception) {
            logger.error("Blockwise MCP endpoint failed to start; MCP is disabled for this server session", exception);
        }
    }

    public boolean isRunning() {
        return mcpServer != null;
    }

    public void serverStopping(ServerStoppingEvent event) {
        if (mcpServer == null) {
            return;
        }
        try {
            mcpServer.close();
        } catch (RuntimeException exception) {
            logger.error("Blockwise MCP endpoint failed to stop cleanly", exception);
        } finally {
            mcpServer = null;
        }
    }
}
