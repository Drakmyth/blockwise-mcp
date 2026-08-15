package com.drakmyth.minecraft.blockwisemcp.mcp.tools;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

final class ToolResults {
    private ToolResults() {
    }

    static CallToolResult failure(Exception exception) {
        return CallToolResult.builder()
                .isError(true)
                .addTextContent(ToolError.from(exception).message())
                .build();
    }
}
