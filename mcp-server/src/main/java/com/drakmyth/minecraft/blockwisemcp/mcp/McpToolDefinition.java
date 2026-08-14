package com.drakmyth.minecraft.blockwisemcp.mcp;

/** Loader-independent handle for a tool registered with the embedded MCP server. */
public interface McpToolDefinition {
    /** Returns the SDK-specific definition for internal server registration. */
    Object sdkDefinition();
}
