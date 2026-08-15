package com.drakmyth.minecraft.blockwisemcp.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.drakmyth.minecraft.blockwisemcp.core.ids.ResourceId;
import com.drakmyth.minecraft.blockwisemcp.core.mods.LoadedMod;
import com.drakmyth.minecraft.blockwisemcp.core.mods.ModService;
import com.drakmyth.minecraft.blockwisemcp.core.recipes.IngredientOption;
import com.drakmyth.minecraft.blockwisemcp.core.recipes.RecipeDefinition;
import com.drakmyth.minecraft.blockwisemcp.core.recipes.RecipeIngredient;
import com.drakmyth.minecraft.blockwisemcp.core.recipes.RecipeInput;
import com.drakmyth.minecraft.blockwisemcp.core.recipes.RecipeOutput;
import com.drakmyth.minecraft.blockwisemcp.core.recipes.RecipeService;
import com.drakmyth.minecraft.blockwisemcp.core.recipes.RecipeSnapshot;
import com.drakmyth.minecraft.blockwisemcp.mcp.tools.FindRecipesByOutputTool;
import com.drakmyth.minecraft.blockwisemcp.mcp.tools.ListLoadedModsTool;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class BlockwiseMcpServerTest {
    private static final UUID GENERATION = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID NEXT_GENERATION = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void exposesLoadedModsThroughStreamableHttp() throws Exception {
        var service = new ModService(
                () -> List.of(
                        new LoadedMod("zeta", "Craft Helper", "1"),
                        new LoadedMod("alpha", "Alpha", "2")),
                GENERATION);
        var tools = List.of(ListLoadedModsTool.create(service, directExecutor()));
        try (var server = BlockwiseMcpServer.start(0, Duration.ofSeconds(5), "test", tools);
                var client = McpClient.sync(HttpClientStreamableHttpTransport.builder(
                                "http://" + BlockwiseMcpServer.HOST + ":" + server.port())
                        .endpoint(BlockwiseMcpServer.ENDPOINT)
                        .build()).build()) {
            var initialization = client.initialize();
            assertEquals("blockwise-mcp", initialization.serverInfo().name());
            assertEquals(BlockwiseMcpServer.INSTRUCTIONS, initialization.instructions());

            var listedTools = client.listTools();
            assertEquals(List.of("list_loaded_mods"), listedTools.tools().stream().map(tool -> tool.name()).toList());

            var first = client.callTool(CallToolRequest.builder("list_loaded_mods")
                    .arguments(Map.of("limit", 1))
                    .build());
            assertFalse(first.isError());
            var output = asMap(first.structuredContent());
            assertEquals("alpha", firstItemId(output));
            assertNotNull(output.get("nextCursor"));

            var second = client.callTool(CallToolRequest.builder("list_loaded_mods")
                    .arguments(Map.of("limit", 1, "cursor", output.get("nextCursor")))
                    .build());
            assertEquals("zeta", firstItemId(asMap(second.structuredContent())));

            var invalid = client.callTool(CallToolRequest.builder("list_loaded_mods")
                    .arguments(Map.of("limit", "many"))
                    .build());
            assertTrue(invalid.isError());
        }
    }

    @Test
    void exposesRecipesThroughStreamableHttp() throws Exception {
        var outputId = ResourceId.parse("minecraft:result");
        var generation = GENERATION;
        var ingredient = new RecipeIngredient(List.of(
                new IngredientOption.Item(ResourceId.parse("minecraft:coal")),
                new IngredientOption.Tag(ResourceId.parse("c:coals"))));
        var recipes = List.of(
                recipe("example:shaped", new RecipeInput.Shaped(List.of(java.util.Arrays.asList(ingredient, null))), outputId),
                recipe("example:shapeless", new RecipeInput.Shapeless(List.of(ingredient)), outputId),
                recipe("example:single", new RecipeInput.Single(ingredient), outputId),
                recipe(
                        "example:ignored",
                        new RecipeInput.Single(ingredient),
                        ResourceId.parse("minecraft:other_result")));
        var service = new RecipeService(() -> new RecipeSnapshot(generation, recipes));
        var tools = List.of(FindRecipesByOutputTool.create(service, directExecutor()));
        try (var server = BlockwiseMcpServer.start(0, Duration.ofSeconds(5), "test", tools);
                var client = McpClient.sync(HttpClientStreamableHttpTransport.builder(
                                "http://" + BlockwiseMcpServer.HOST + ":" + server.port())
                        .endpoint(BlockwiseMcpServer.ENDPOINT)
                        .build()).build()) {
            client.initialize();

            var first = client.callTool(CallToolRequest.builder("find_recipes_by_output")
                    .arguments(Map.of("outputItemId", outputId.toString(), "limit", 2))
                    .build());
            assertFalse(first.isError());
            var firstOutput = asMap(first.structuredContent());
            var firstItems = (List<?>) firstOutput.get("items");
            assertEquals(List.of("shaped", "shapeless"), firstItems.stream()
                    .map(item -> asMap(asMap(item).get("input")).get("format"))
                    .toList());
            var shapedRows = (List<?>) asMap(asMap(firstItems.getFirst()).get("input")).get("rows");
            var shapedRow = (List<?>) shapedRows.getFirst();
            assertEquals(List.of("minecraft:coal", "#c:coals"),
                    asMap(shapedRow.getFirst()).get("options"));
            assertEquals(null, shapedRow.get(1));
            assertNotNull(firstOutput.get("nextCursor"));

            var second = client.callTool(CallToolRequest.builder("find_recipes_by_output")
                    .arguments(Map.of(
                            "outputItemId", outputId.toString(),
                            "limit", 2,
                            "cursor", firstOutput.get("nextCursor")))
                    .build());
            var secondItem = asMap(((List<?>) asMap(second.structuredContent()).get("items")).getFirst());
            assertEquals("single", asMap(secondItem.get("input")).get("format"));

            var invalid = client.callTool(CallToolRequest.builder("find_recipes_by_output")
                    .arguments(Map.of("outputItemId", "result"))
                    .build());
            assertTrue(invalid.isError());
        }
    }

    @Test
    void reportsLoadedModSourceFailureAsToolError() throws Exception {
        var service = new ModService(
                () -> {
                    throw new IllegalStateException("Loaded mod source failed");
                },
                GENERATION);
        var tools = List.of(ListLoadedModsTool.create(service, directExecutor()));
        try (var server = BlockwiseMcpServer.start(0, Duration.ofSeconds(5), "test", tools);
                var client = McpClient.sync(HttpClientStreamableHttpTransport.builder(
                                "http://" + BlockwiseMcpServer.HOST + ":" + server.port())
                        .endpoint(BlockwiseMcpServer.ENDPOINT)
                        .build()).build()) {
            client.initialize();

            var failed = client.callTool(CallToolRequest.builder("list_loaded_mods").arguments(Map.of()).build());
            assertTrue(failed.isError());
            assertEquals(null, failed.structuredContent());
            assertEquals(
                    "INTERNAL_ERROR: Blockwise MCP could not complete the request. Do not retry unchanged; consult the server logs or report the failure.",
                    ((TextContent) failed.content().getFirst()).text());
        }
    }

    @Test
    void reportsStaleRecipeCursorAsToolError() throws Exception {
        var generation = new AtomicReference<>(GENERATION);
        var outputId = ResourceId.parse("minecraft:result");
        var ingredient = new RecipeIngredient(List.of(
                new IngredientOption.Item(ResourceId.parse("minecraft:coal"))));
        var recipes = List.of(
                recipe("example:first", new RecipeInput.Single(ingredient), outputId),
                recipe("example:second", new RecipeInput.Single(ingredient), outputId));
        var service = new RecipeService(() -> new RecipeSnapshot(generation.get(), recipes));
        var tools = List.of(FindRecipesByOutputTool.create(service, directExecutor()));
        try (var server = BlockwiseMcpServer.start(0, Duration.ofSeconds(5), "test", tools);
                var client = McpClient.sync(HttpClientStreamableHttpTransport.builder(
                                "http://" + BlockwiseMcpServer.HOST + ":" + server.port())
                        .endpoint(BlockwiseMcpServer.ENDPOINT)
                        .build()).build()) {
            client.initialize();

            var first = client.callTool(CallToolRequest.builder("find_recipes_by_output")
                    .arguments(Map.of("outputItemId", outputId.toString(), "limit", 1))
                    .build());
            var cursor = asMap(first.structuredContent()).get("nextCursor");
            assertNotNull(cursor);

            generation.set(NEXT_GENERATION);
            var stale = client.callTool(CallToolRequest.builder("find_recipes_by_output")
                    .arguments(Map.of("outputItemId", outputId.toString(), "limit", 1, "cursor", cursor))
                    .build());
            assertTrue(stale.isError());
            assertEquals(null, stale.structuredContent());
            assertEquals(
                    "CURSOR_STALE: Runtime data changed after the cursor was issued. Repeat the same query without a cursor.",
                    ((TextContent) stale.content().getFirst()).text());
        }
    }

    @Test
    void describesEveryPublishedSchemaProperty() throws Exception {
        var modService = new ModService(() -> List.of(), GENERATION);
        var recipeService = new RecipeService(() -> new RecipeSnapshot(GENERATION, List.of()));
        var tools = List.of(
                ListLoadedModsTool.create(modService, directExecutor()),
                FindRecipesByOutputTool.create(recipeService, directExecutor()));
        try (var server = BlockwiseMcpServer.start(0, Duration.ofSeconds(5), "test", tools);
                var client = McpClient.sync(HttpClientStreamableHttpTransport.builder(
                                "http://" + BlockwiseMcpServer.HOST + ":" + server.port())
                        .endpoint(BlockwiseMcpServer.ENDPOINT)
                        .build()).build()) {
            client.initialize();

            for (var tool : client.listTools().tools()) {
                assertPropertyDescriptions(tool.name() + " input", tool.inputSchema());
                assertPropertyDescriptions(tool.name() + " output", tool.outputSchema());
            }
        }
    }

    private static McpToolExecutor directExecutor() {
        return new McpToolExecutor() {
            @Override
            public <T> T execute(Callable<T> operation) throws Exception {
                return operation.call();
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return (Map<String, Object>) value;
    }

    private static void assertPropertyDescriptions(String path, Map<String, Object> schema) {
        var properties = asMap(schema.get("properties"));
        for (var entry : properties.entrySet()) {
            var propertyPath = path + "." + entry.getKey();
            var propertySchema = asMap(entry.getValue());
            var description = propertySchema.get("description");
            assertTrue(
                    description instanceof String text && !text.isBlank(),
                    propertyPath + " has no description");
            assertNestedPropertyDescriptions(propertyPath, propertySchema);
        }
    }

    private static void assertNestedPropertyDescriptions(String path, Map<String, Object> schema) {
        if (schema.containsKey("properties")) {
            assertPropertyDescriptions(path, schema);
        }
        if (schema.get("items") instanceof Map<?, ?> itemSchema) {
            assertNestedPropertyDescriptions(path + "[]", asMap(itemSchema));
        }
        if (schema.get("oneOf") instanceof List<?> variants) {
            for (var index = 0; index < variants.size(); index++) {
                assertNestedPropertyDescriptions(path + ".oneOf[" + index + "]", asMap(variants.get(index)));
            }
        }
    }

    private static RecipeDefinition recipe(String id, RecipeInput input, ResourceId outputId) {
        return new RecipeDefinition(
                ResourceId.parse(id),
                ResourceId.parse("minecraft:test_serializer"),
                input,
                List.of(new RecipeOutput(outputId, 1)));
    }

    private static String firstItemId(Map<String, Object> output) {
        var items = (List<?>) output.get("items");
        return (String) asMap(items.getFirst()).get("id");
    }
}
