package com.drakmyth.minecraft.blockwisemcp.mcp;

import java.util.concurrent.Callable;

/** Dispatches MCP tool work to the runtime thread that owns authoritative state. */
@FunctionalInterface
public interface McpToolExecutor {
    /** Executes one tool operation and returns its result. */
    <T> T execute(Callable<T> operation) throws Exception;
}
