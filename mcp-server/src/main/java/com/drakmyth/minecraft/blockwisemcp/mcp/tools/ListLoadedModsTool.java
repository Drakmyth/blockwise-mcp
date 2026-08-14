package com.drakmyth.minecraft.blockwisemcp.mcp.tools;

import com.drakmyth.minecraft.blockwisemcp.core.mods.ListLoadedModsRequest;
import com.drakmyth.minecraft.blockwisemcp.core.mods.LoadedMod;
import com.drakmyth.minecraft.blockwisemcp.core.mods.ModService;
import com.drakmyth.minecraft.blockwisemcp.mcp.McpToolDefinition;
import com.drakmyth.minecraft.blockwisemcp.mcp.McpToolExecutor;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

public final class ListLoadedModsTool {
    private static final Map<String, Object> INPUT_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "filter", Map.of("type", "string", "description", "ID or display-name substring"),
                    "limit", Map.of("type", "integer", "minimum", 1, "maximum", 100),
                    "cursor", Map.of("type", "string", "description", "Opaque continuation cursor")),
            "additionalProperties", false);

    private static final Set<String> INPUT_FIELDS = Set.of("filter", "limit", "cursor");

    private static final Map<String, Object> OUTPUT_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "items", Map.of(
                            "type", "array",
                            "items", Map.of(
                                    "type", "object",
                                    "properties", Map.of(
                                            "id", Map.of("type", "string"),
                                            "displayName", Map.of("type", "string"),
                                            "version", Map.of("type", "string")),
                                    "required", List.of("id", "displayName", "version"),
                                    "additionalProperties", false)),
                    "nextCursor", Map.of("type", List.of("string", "null"))),
            "required", List.of("items", "nextCursor"),
            "additionalProperties", false);

    private ListLoadedModsTool() {
    }

    public static McpToolDefinition create(ModService service, McpToolExecutor executor) {
        var tool = Tool.builder("list_loaded_mods", INPUT_SCHEMA)
                .description("Lists mods loaded in the current Minecraft runtime")
                .outputSchema(OUTPUT_SCHEMA)
                .build();
        var definition = SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((exchange, call) -> invoke(service, executor, call.arguments()))
                .build();
        return () -> definition;
    }

    private static CallToolResult invoke(ModService service, McpToolExecutor executor, Map<String, Object> arguments) {
        try {
            if (!INPUT_FIELDS.containsAll(arguments.keySet())) {
                throw new IllegalArgumentException("Input contains an unsupported field");
            }
            var request = new ListLoadedModsRequest(
                    optionalString(arguments, "filter"),
                    optionalInteger(arguments, "limit"),
                    optionalString(arguments, "cursor"));
            var page = executor.execute(() -> service.listLoadedMods(request));
            var items = page.items().stream().map(ListLoadedModsTool::toMap).toList();
            var output = new LinkedHashMap<String, Object>();
            output.put("items", items);
            output.put("nextCursor", page.nextCursor());
            return CallToolResult.builder().structuredContent(output).build();
        } catch (Exception exception) {
            return CallToolResult.builder().isError(true).structuredContent(Map.of(
                    "code", "TOOL_EXECUTION_FAILED",
                    "message", exception.getMessage() == null ? "Tool execution failed" : exception.getMessage()))
                    .build();
        }
    }

    private static String optionalString(Map<String, Object> arguments, String name) {
        var value = arguments.get(name);
        if (value == null || value instanceof String) {
            return (String) value;
        }
        throw new IllegalArgumentException(name + " must be a string");
    }

    private static Integer optionalInteger(Map<String, Object> arguments, String name) {
        var value = arguments.get(name);
        if (value == null || value instanceof Integer) {
            return (Integer) value;
        }
        throw new IllegalArgumentException(name + " must be an integer");
    }

    private static Map<String, Object> toMap(LoadedMod mod) {
        return Map.of("id", mod.id(), "displayName", mod.displayName(), "version", mod.version());
    }
}
