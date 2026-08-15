package com.drakmyth.minecraft.blockwisemcp.mcp.tools;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

final class ToolResults {
    private ToolResults() {
    }

    static CallToolResult failure(Exception exception) {
        var message = exception.getMessage() == null ? "Tool execution failed" : exception.getMessage();
        return CallToolResult.builder()
                .isError(true)
                .addTextContent(message)
                .build();
    }
}
