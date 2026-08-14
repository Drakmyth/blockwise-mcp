package com.drakmyth.minecraft.blockwisemcp.mcp;

import com.drakmyth.minecraft.blockwisemcp.core.mods.ModService;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.DefaultServerTransportSecurityValidator;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

/** Embedded loader-independent Streamable HTTP server for Blockwise MCP tools. */
public final class BlockwiseMcpServer implements AutoCloseable {
    public static final String HOST = "127.0.0.1";
    public static final String ENDPOINT = "/mcp";

    private final HttpServletStreamableServerTransportProvider transport;
    private final McpSyncServer mcpServer;
    private final Tomcat tomcat;
    private final Path baseDirectory;

    private BlockwiseMcpServer(
            HttpServletStreamableServerTransportProvider transport,
            McpSyncServer mcpServer,
            Tomcat tomcat,
            Path baseDirectory) {
        this.transport = transport;
        this.mcpServer = mcpServer;
        this.tomcat = tomcat;
        this.baseDirectory = baseDirectory;
    }

    /** Starts an endpoint bound strictly to {@value #HOST}. */
    public static BlockwiseMcpServer start(
            int port,
            Duration requestTimeout,
            String version,
            ModService modService,
            McpToolExecutor executor) throws IOException, LifecycleException {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("port must be between 0 and 65535");
        }
        Objects.requireNonNull(requestTimeout, "requestTimeout");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(modService, "modService");
        Objects.requireNonNull(executor, "executor");

        var security = DefaultServerTransportSecurityValidator.builder()
                .allowedHost(HOST + ":*")
                .allowedOrigin("http://" + HOST + ":*")
                .build();
        var transport = HttpServletStreamableServerTransportProvider.builder()
                .mcpEndpoint(ENDPOINT)
                .securityValidator(security)
                .build();
        var mcpServer = McpServer.sync(transport)
                .serverInfo("blockwise-mcp", version)
                .capabilities(ServerCapabilities.builder().tools(false).build())
                .tools(LoadedModsTool.create(modService, executor))
                .requestTimeout(requestTimeout)
                .build();
        var baseDirectory = Files.createTempDirectory("blockwise-mcp-");
        var tomcat = createTomcat(port, transport, baseDirectory);
        try {
            tomcat.start();
            return new BlockwiseMcpServer(transport, mcpServer, tomcat, baseDirectory);
        } catch (LifecycleException exception) {
            mcpServer.close();
            deleteBaseDirectory(baseDirectory);
            throw exception;
        }
    }

    /** Returns the bound port, including the allocated port when started with zero. */
    public int port() {
        return tomcat.getConnector().getLocalPort();
    }

    @Override
    public void close() {
        mcpServer.closeGracefully();
        try {
            tomcat.stop();
            tomcat.destroy();
        } catch (LifecycleException exception) {
            throw new IllegalStateException("Failed to stop MCP server", exception);
        } finally {
            transport.closeGracefully().block();
            deleteBaseDirectory(baseDirectory);
        }
    }

    private static Tomcat createTomcat(
            int port, HttpServletStreamableServerTransportProvider transport, Path baseDirectory) {
        var tomcat = new Tomcat();
        tomcat.setBaseDir(baseDirectory.toString());
        tomcat.setPort(port);
        Context context = tomcat.addContext("", baseDirectory.toString());
        var wrapper = context.createWrapper();
        wrapper.setName("mcp");
        wrapper.setServlet(transport);
        wrapper.setLoadOnStartup(1);
        wrapper.setAsyncSupported(true);
        context.addChild(wrapper);
        context.addServletMappingDecoded("/*", "mcp");
        var connector = tomcat.getConnector();
        connector.setProperty("address", HOST);
        connector.setMaxPostSize(1_048_576);
        return tomcat;
    }

    private static void deleteBaseDirectory(Path directory) {
        try {
            Files.deleteIfExists(directory);
        } catch (IOException ignored) {
            // Tomcat may leave temporary files for process cleanup.
        }
    }
}
