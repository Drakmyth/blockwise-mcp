package com.drakmyth.minecraft.blockwisemcp.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.drakmyth.minecraft.blockwisemcp.core.mods.LoadedMod;
import com.drakmyth.minecraft.blockwisemcp.core.mods.ModService;
import com.drakmyth.minecraft.blockwisemcp.mcp.tools.ListLoadedModsTool;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;

class BlockwiseMcpServerTest {
    @Test
    void exposesLoadedModsThroughStreamableHttp() throws Exception {
        var service = new ModService(
                () -> List.of(
                        new LoadedMod("zeta", "Craft Helper", "1"),
                        new LoadedMod("alpha", "Alpha", "2")),
                1);
        var tools = List.of(ListLoadedModsTool.create(service, directExecutor()));
        try (var server = BlockwiseMcpServer.start(0, Duration.ofSeconds(5), "test", tools);
                var client = McpClient.sync(HttpClientStreamableHttpTransport.builder(
                                "http://" + BlockwiseMcpServer.HOST + ":" + server.port())
                        .endpoint(BlockwiseMcpServer.ENDPOINT)
                        .build()).build()) {
            var initialization = client.initialize();
            assertEquals("blockwise-mcp", initialization.serverInfo().name());

            var listedTools = client.listTools();
            assertEquals(List.of("list_loaded_mods"), listedTools.tools().stream().map(tool -> tool.name()).toList());
            var outputProperties = asMap(listedTools.tools().getFirst().outputSchema().get("properties"));
            assertEquals("Loaded mods in this page", asMap(outputProperties.get("items")).get("description"));
            assertEquals(
                    "Opaque cursor for the next page, or null when no more mods remain",
                    asMap(outputProperties.get("nextCursor")).get("description"));

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

    private static String firstItemId(Map<String, Object> output) {
        var items = (List<?>) output.get("items");
        return (String) asMap(items.getFirst()).get("id");
    }
}
