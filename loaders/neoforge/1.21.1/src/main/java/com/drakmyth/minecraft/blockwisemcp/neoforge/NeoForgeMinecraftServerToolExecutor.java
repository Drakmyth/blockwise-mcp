package com.drakmyth.minecraft.blockwisemcp.neoforge;

import com.drakmyth.minecraft.blockwisemcp.mcp.McpToolExecutor;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import net.minecraft.server.MinecraftServer;

/** Executes MCP tool operations on the authoritative Minecraft server thread. */
public final class NeoForgeMinecraftServerToolExecutor implements McpToolExecutor {
    private final MinecraftServer server;
    private final Duration timeout;

    public NeoForgeMinecraftServerToolExecutor(MinecraftServer server, Duration timeout) {
        this.server = Objects.requireNonNull(server, "server");
        this.timeout = Objects.requireNonNull(timeout, "timeout");
    }

    @Override
    public <T> T execute(Callable<T> operation) throws Exception {
        Objects.requireNonNull(operation, "operation");
        var result = new CompletableFuture<T>();
        server.execute(() -> {
            try {
                result.complete(operation.call());
            } catch (Exception exception) {
                result.completeExceptionally(exception);
            }
        });
        try {
            return result.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw exception;
        } catch (ExecutionException exception) {
            if (exception.getCause() instanceof Exception cause) {
                throw cause;
            }
            throw exception;
        }
    }
}
