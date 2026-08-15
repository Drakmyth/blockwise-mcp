package com.drakmyth.minecraft.blockwisemcp.fabric;

import com.drakmyth.minecraft.blockwisemcp.core.mods.ModService;
import com.drakmyth.minecraft.blockwisemcp.core.recipes.RecipeService;
import com.drakmyth.minecraft.blockwisemcp.mcp.BlockwiseMcpServer;
import com.drakmyth.minecraft.blockwisemcp.mcp.tools.FindRecipesByOutputTool;
import com.drakmyth.minecraft.blockwisemcp.mcp.tools.ListLoadedModsTool;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

/** Composes and owns the MCP endpoint for each active Fabric server. */
final class FabricMcpLifecycle {
    private final Logger logger;
    private final String version;
    private final FabricConfig config;
    private BlockwiseMcpServer mcpServer;
    private FabricRecipeSource recipeSource;
    private RecipeService recipeService;

    FabricMcpLifecycle(Logger logger, String version, FabricConfig config) {
        this.logger = logger;
        this.version = version;
        this.config = config;
    }

    void serverStarted(MinecraftServer server) {
        if (!config.enabled()) {
            logger.info("Blockwise MCP endpoint is disabled");
            return;
        }

        var timeout = config.dispatchTimeout();
        var modService = new ModService(new FabricLoadedModSource(), UUID.randomUUID());
        recipeSource = new FabricRecipeSource(server);
        recipeService = new RecipeService(recipeSource);
        var executor = new MinecraftServerToolExecutor(server, timeout);
        var tools = List.of(
                ListLoadedModsTool.create(modService, executor),
                FindRecipesByOutputTool.create(recipeService, executor));
        try {
            mcpServer = BlockwiseMcpServer.start(config.port(), timeout, version, tools);
            logger.info("Blockwise MCP endpoint started at http://127.0.0.1:{}/mcp", mcpServer.port());
        } catch (Exception | LinkageError exception) {
            logger.error("Blockwise MCP endpoint failed to start; MCP is disabled for this server session", exception);
        }
    }

    void dataPackReloaded(boolean successful) {
        if (recipeSource != null && successful) {
            recipeSource.advanceGeneration();
            logger.info("Blockwise recipe cursors invalidated after server-data reload");
        }
    }

    boolean isRunning() {
        return mcpServer != null;
    }

    void serverStopping() {
        recipeService = null;
        recipeSource = null;
        if (mcpServer != null) {
            logger.info("Blockwise MCP endpoint stopping");
            try {
                mcpServer.close();
                logger.info("Blockwise MCP endpoint stopped");
            } catch (RuntimeException exception) {
                logger.error("Blockwise MCP endpoint failed to stop cleanly", exception);
            } finally {
                mcpServer = null;
            }
        }
    }
}
