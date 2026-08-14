package com.drakmyth.minecraft.blockwisemcp.neoforge;

import com.drakmyth.minecraft.blockwisemcp.core.mods.ModService;
import com.drakmyth.minecraft.blockwisemcp.core.recipes.RecipeService;
import com.drakmyth.minecraft.blockwisemcp.mcp.BlockwiseMcpServer;
import com.drakmyth.minecraft.blockwisemcp.mcp.tools.ListLoadedModsTool;
import java.util.List;
import java.util.UUID;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;

/** Composes and owns the MCP endpoint for each active Minecraft server. */
public final class NeoForgeMcpLifecycle {
    private final Logger logger;
    private final String version;
    private final BlockwiseConfig config;
    private BlockwiseMcpServer mcpServer;
    private NeoForgeRecipeSource recipeSource;
    private RecipeService recipeService;

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
        var modService = new ModService(new NeoForgeLoadedModSource(), UUID.randomUUID());
        recipeSource = new NeoForgeRecipeSource(event.getServer());
        recipeService = new RecipeService(recipeSource);
        var executor = new MinecraftServerToolExecutor(event.getServer(), timeout);
        var tools = List.of(ListLoadedModsTool.create(modService, executor));
        try {
            mcpServer = BlockwiseMcpServer.start(config.port(), timeout, version, tools);
            logger.info("Blockwise MCP endpoint started at http://127.0.0.1:{}/mcp", mcpServer.port());
        } catch (Exception | LinkageError exception) {
            logger.error("Blockwise MCP endpoint failed to start; MCP is disabled for this server session", exception);
        }
    }

    /** Invalidates recipe cursors after NeoForge publishes a successful server-data reload. */
    public void tagsUpdated(TagsUpdatedEvent event) {
        if (recipeSource != null && event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) {
            recipeSource.advanceGeneration();
        }
    }

    public boolean isRunning() {
        return mcpServer != null;
    }

    public void serverStopping(ServerStoppingEvent event) {
        recipeService = null;
        recipeSource = null;
        if (mcpServer != null) {
            try {
                mcpServer.close();
            } catch (RuntimeException exception) {
                logger.error("Blockwise MCP endpoint failed to stop cleanly", exception);
            } finally {
                mcpServer = null;
            }
        }
    }
}
