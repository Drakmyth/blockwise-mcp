package com.drakmyth.minecraft.blockwisemcp.contracttests;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator.ValidationResponse;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
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
