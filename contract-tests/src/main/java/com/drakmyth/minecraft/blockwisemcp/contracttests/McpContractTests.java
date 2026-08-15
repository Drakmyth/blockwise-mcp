package com.drakmyth.minecraft.blockwisemcp.contracttests;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator.ValidationResponse;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Loader-neutral assertions against the public Blockwise MCP endpoint. */
public final class McpContractTests {
    private static final String BASE_URL = "http://127.0.0.1:47831";
    private static final String ENDPOINT = "/mcp";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private McpContractTests() {
    }

    /** Verifies MCP initialization and tool discovery through the real Streamable HTTP endpoint. */
    public static void verifyDiscovery(String expectedVersion) {
        try (var client = openInitializedClient()) {
            var initialization = client.getCurrentInitializationResult();
            requireEquals("blockwise-mcp", initialization.serverInfo().name(), "server name");
            requireEquals(expectedVersion, initialization.serverInfo().version(), "server version");
            require(
                    initialization.instructions() != null && !initialization.instructions().isBlank(),
                    "server instructions must be present");
            require(initialization.capabilities().tools() != null, "server must advertise tool capabilities");
            require(initialization.capabilities().resources() == null, "server must not advertise resources");
            require(initialization.capabilities().prompts() == null, "server must not advertise prompts");

            var tools = client.listTools().tools();
            requireEquals(
                    List.of("find_recipes_by_output", "list_loaded_mods"),
                    tools.stream().map(Tool::name).sorted().toList(),
                    "tool names");
            verifyTool(
                    tool(tools, "list_loaded_mods"),
                    "Lists mods loaded in the current Minecraft runtime",
                    Set.of("filter", "limit", "cursor"),
                    Set.of("items", "nextCursor"));
            verifyTool(
                    tool(tools, "find_recipes_by_output"),
                    "Finds loaded recipes that produce an exact item ID",
                    Set.of("outputItemId", "limit", "cursor"),
                    Set.of("items", "nextCursor"));
        }
    }

    /** Verifies loaded-mod results through the real Streamable HTTP endpoint. */
    public static void verifyLoadedMods(String expectedVersion) {
        try (var client = openInitializedClient()) {
            var result = client.callTool(CallToolRequest.builder("list_loaded_mods")
                    .arguments(Map.of("filter", "Blockwise MCP"))
                    .build());
            require(!Boolean.TRUE.equals(result.isError()), "list_loaded_mods must succeed");
            var output = map(result.structuredContent(), "loaded-mod output");
            var items = list(output.get("items"), "loaded-mod items");
            var modIds = items.stream().map(item -> map(item, "loaded mod").get("id")).toList();
            requireEquals(List.of("blockwisemcp", "blockwisemcp_gametest"), modIds, "Blockwise loaded-mod IDs");
            requireEquals(expectedVersion, map(items.get(0), "production mod").get("version"), "production mod version");
            requireEquals("Blockwise MCP", map(items.get(0), "production mod").get("displayName"), "production display name");
            requireEquals(
                    "Blockwise MCP GameTest",
                    map(items.get(1), "GameTest mod").get("displayName"),
                    "GameTest display name");
            requireEquals(null, output.get("nextCursor"), "loaded-mod cursor");
        }
    }

    /** Verifies deterministic recipe results and pagination through the real MCP endpoint. */
    public static void verifyRecipes() {
        var transport = HttpClientStreamableHttpTransport.builder(BASE_URL)
                .endpoint(ENDPOINT)
                .connectTimeout(TIMEOUT)
                .build();
        try (var client = McpClient.sync(transport)
                .initializationTimeout(TIMEOUT)
                .requestTimeout(TIMEOUT)
                .jsonSchemaValidator((schema, value) -> ValidationResponse.asValid(""))
                .build()) {
            client.initialize();
            var expectedResponse = expectedRecipesResponse();
            var expectedItems = list(expectedResponse.get("items"), "expected recipe items");
            var unpaged = client.callTool(CallToolRequest.builder("find_recipes_by_output")
                    .arguments(Map.of("outputItemId", "minecraft:debug_stick"))
                    .build());
            require(!Boolean.TRUE.equals(unpaged.isError()), "unpaged recipe call must succeed");
            requireEquals(1, unpaged.content().size(), "successful recipe text-content count");
            require(unpaged.content().getFirst() instanceof TextContent, "successful recipe content must be text");
            var text = ((TextContent) unpaged.content().getFirst()).text();
            require(text.contains("blockwisemcp:contract_shaped"), "recipe text content must mirror fixtures");
            var unpagedOutput = map(unpaged.structuredContent(), "unpaged recipe output");
            requireEquals(expectedResponse, unpagedOutput, "recipe response");

            var paged = new java.util.ArrayList<Object>();
            Object cursor = null;
            for (var index = 0; index < expectedItems.size(); index++) {
                var arguments = new java.util.LinkedHashMap<String, Object>();
                arguments.put("outputItemId", "minecraft:debug_stick");
                arguments.put("limit", 1);
                if (cursor != null) {
                    arguments.put("cursor", cursor);
                }
                var page = client.callTool(CallToolRequest.builder("find_recipes_by_output")
                        .arguments(arguments)
                        .build());
                require(!Boolean.TRUE.equals(page.isError()), "recipe page " + index + " must succeed");
                var pageOutput = map(page.structuredContent(), "recipe page " + index);
                var items = list(pageOutput.get("items"), "recipe page items");
                requireEquals(1, items.size(), "recipe page size");
                paged.add(items.getFirst());
                cursor = pageOutput.get("nextCursor");
                require(
                        index == expectedItems.size() - 1 ? cursor == null : cursor instanceof String,
                        "recipe page " + index + " cursor state");
            }
            requireEquals(expectedItems, paged, "paginated recipe fixtures");
        }
    }

    // Keep expected MCP output independent from recipe JSON inputs so fixture or mapping drift fails visibly.
    private static Map<String, Object> expectedRecipesResponse() {
        var output = List.of(Map.of("itemId", "minecraft:debug_stick", "count", 1));
        var items = List.of(
                Map.of(
                        "id", "blockwisemcp:contract_shaped",
                        "type", "minecraft:crafting_shaped",
                        "input", Map.of(
                                "format", "shaped",
                                "rows", List.of(
                                        List.of(Map.of("options", List.of("minecraft:dirt"))),
                                        List.of(Map.of("options", List.of("#minecraft:planks"))))),
                        "outputs", output),
                Map.of(
                        "id", "blockwisemcp:contract_shapeless",
                        "type", "minecraft:crafting_shapeless",
                        "input", Map.of(
                                "format", "shapeless",
                                "ingredients", List.of(Map.of("options", List.of("minecraft:cobblestone")))),
                        "outputs", output),
                Map.of(
                        "id", "blockwisemcp:contract_smelting",
                        "type", "minecraft:smelting",
                        "input", Map.of(
                                "format", "single",
                                "ingredient", Map.of("options", List.of("minecraft:sand"))),
                        "outputs", output),
                Map.of(
                        "id", "blockwisemcp:contract_stonecutting",
                        "type", "minecraft:stonecutting",
                        "input", Map.of(
                                "format", "single",
                                "ingredient", Map.of("options", List.of("minecraft:stone"))),
                        "outputs", output));
        var response = new java.util.LinkedHashMap<String, Object>();
        response.put("items", items);
        response.put("nextCursor", null);
        return response;
    }

    private static McpSyncClient openInitializedClient() {
        var transport = HttpClientStreamableHttpTransport.builder(BASE_URL)
                .endpoint(ENDPOINT)
                .connectTimeout(TIMEOUT)
                .build();
        var client = McpClient.sync(transport)
                .initializationTimeout(TIMEOUT)
                .requestTimeout(TIMEOUT)
                // Server-side schema validation remains authoritative; avoid loader service-provider class identity issues.
                .jsonSchemaValidator((schema, value) -> ValidationResponse.asValid(""))
                .build();
        client.initialize();
        return client;
    }

    private static void verifyTool(
            Tool tool,
            String description,
            Set<String> inputProperties,
            Set<String> outputProperties) {
        requireEquals(description, tool.description(), tool.name() + " description");
        verifyObjectSchema(tool.name() + " input", tool.inputSchema(), inputProperties);
        verifyObjectSchema(tool.name() + " output", tool.outputSchema(), outputProperties);
    }

    private static void verifyObjectSchema(String label, Map<String, Object> schema, Set<String> properties) {
        requireEquals("object", schema.get("type"), label + " type");
        requireEquals(false, schema.get("additionalProperties"), label + " additionalProperties");
        requireEquals(properties, map(schema.get("properties"), label + " properties").keySet(), label + " properties");
    }

    private static Tool tool(List<Tool> tools, String name) {
        return tools.stream().filter(tool -> name.equals(tool.name())).findFirst()
                .orElseThrow(() -> new AssertionError("Missing tool: " + name));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value, String label) {
        require(value instanceof Map<?, ?>, label + " must be an object");
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Object value, String label) {
        require(value instanceof List<?>, label + " must be an array");
        return (List<Object>) value;
    }

    private static void requireEquals(Object expected, Object actual, String label) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(label + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
