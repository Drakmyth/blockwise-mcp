package com.drakmyth.minecraft.blockwisemcp.mcp.tools;

import com.drakmyth.minecraft.blockwisemcp.core.mods.ListLoadedModsRequest;
import com.drakmyth.minecraft.blockwisemcp.core.mods.LoadedMod;
import com.drakmyth.minecraft.blockwisemcp.core.mods.ModService;
import com.drakmyth.minecraft.blockwisemcp.core.pagination.InvalidCursorException;
import com.drakmyth.minecraft.blockwisemcp.mcp.McpToolDefinition;
import com.drakmyth.minecraft.blockwisemcp.mcp.McpToolExecutor;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ListLoadedModsTool {
    private static final Logger LOGGER = LoggerFactory.getLogger(ListLoadedModsTool.class);
    private static final Map<String, Object> INPUT_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "filter", Map.of(
                            "type", "string",
                            "description", "Case-insensitive mod ID or display-name substring"),
                    "limit", Map.of(
                            "type", "integer",
                            "minimum", 1,
                            "maximum", 100,
                            "description", "Maximum mods to return; defaults to 20"),
                    "cursor", Map.of(
                            "type", "string",
                            "description", "Opaque continuation cursor from a previous response")),
            "additionalProperties", false);

    private static final Map<String, Object> OUTPUT_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "items", Map.of(
                            "type", "array",
                            "description", "Loaded mods in this page",
                            "items", Map.of(
                                    "type", "object",
                                    "properties", Map.of(
                                            "id", Map.of(
                                                    "type", "string",
                                                    "description", "Loader-reported mod ID"),
                                            "displayName", Map.of(
                                                    "type", "string",
                                                    "description", "Loader-reported display name"),
                                            "version", Map.of(
                                                    "type", "string",
                                                    "description", "Loader-reported mod version")),
                                    "required", List.of("id", "displayName", "version"),
                                    "additionalProperties", false)),
                    "nextCursor", Map.of(
                            "type", List.of("string", "null"),
                            "description", "Opaque cursor for the next page, or null when no more mods remain")),
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
                .callHandler((exchange, call) -> invoke(service, executor, Arguments.from(call.arguments())))
                .build();
        return () -> definition;
    }

    private static CallToolResult invoke(ModService service, McpToolExecutor executor, Arguments arguments) {
        var startedNanos = System.nanoTime();
        var filter = arguments.filter();
        var limit = arguments.limit();
        var cursor = arguments.cursor();
        try {
            var request = new ListLoadedModsRequest(filter, limit, cursor);
            var page = executor.execute(() -> service.listLoadedMods(request));
            var items = page.items().stream().map(ListLoadedModsTool::toMap).toList();
            var output = new LinkedHashMap<String, Object>();
            output.put("items", items);
            output.put("nextCursor", page.nextCursor());
            LOGGER.debug(
                    "list_loaded_mods filter=\"{}\" limit={} cursorPresent={} results={} hasNext={} durationMs={}",
                    ToolLogging.filter(filter),
                    limit == null ? "<default>" : limit,
                    cursor != null,
                    items.size(),
                    page.nextCursor() != null,
                    ToolLogging.elapsedMillis(startedNanos));
            return CallToolResult.builder().structuredContent(output).build();
        } catch (InvalidCursorException exception) {
            LOGGER.debug(
                    "list_loaded_mods filter=\"{}\" limit={} cursorPresent={} failed reason={} durationMs={}",
                    ToolLogging.filter(filter),
                    limit == null ? "<default>" : limit,
                    cursor != null,
                    exception.reason(),
                    ToolLogging.elapsedMillis(startedNanos));
            return ToolResults.failure(exception);
        } catch (Exception exception) {
            LOGGER.error(
                    "list_loaded_mods filter=\"{}\" limit={} cursorPresent={} failed durationMs={}",
                    ToolLogging.filter(filter),
                    limit == null ? "<default>" : limit,
                    cursor != null,
                    ToolLogging.elapsedMillis(startedNanos),
                    exception);
            return ToolResults.failure(exception);
        }
    }

    private record Arguments(String filter, Integer limit, String cursor) {
        private static Arguments from(Map<String, Object> values) {
            var arguments = new ToolArguments(values);
            return new Arguments(
                    arguments.optionalString("filter"),
                    arguments.optionalInteger("limit"),
                    arguments.optionalString("cursor"));
        }
    }

    private static Map<String, Object> toMap(LoadedMod mod) {
        return Map.of("id", mod.id(), "displayName", mod.displayName(), "version", mod.version());
    }
}
