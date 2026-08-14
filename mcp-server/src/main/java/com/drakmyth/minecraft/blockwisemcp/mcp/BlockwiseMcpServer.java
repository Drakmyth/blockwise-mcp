package com.drakmyth.minecraft.blockwisemcp.mcp;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.DefaultServerTransportSecurityValidator;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.TomcatURLStreamHandlerFactory;

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
            List<McpToolDefinition> tools) throws IOException {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("port must be between 0 and 65535");
        }
        Objects.requireNonNull(requestTimeout, "requestTimeout");
        Objects.requireNonNull(version, "version");
        tools = List.copyOf(Objects.requireNonNull(tools, "tools"));

        var security = DefaultServerTransportSecurityValidator.builder()
                .allowedHost(HOST + ":*")
                .allowedOrigin("http://" + HOST + ":*")
                .build();
        var jsonMapper = McpJsonDefaults.getMapper();
        var transport = HttpServletStreamableServerTransportProvider.builder()
                .jsonMapper(jsonMapper)
                .mcpEndpoint(ENDPOINT)
                .securityValidator(security)
                .build();
        var sdkTools = tools.stream()
                .map(McpToolDefinition::sdkDefinition)
                .map(SyncToolSpecification.class::cast)
                .toList();
        var mcpServer = McpServer.sync(transport)
                .serverInfo("blockwise-mcp", version)
                .capabilities(ServerCapabilities.builder().tools(false).build())
                .jsonMapper(jsonMapper)
                .jsonSchemaValidator(new ModuleCompatibleJsonSchemaValidator())
                .tools(sdkTools)
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
            throw new IOException("Failed to start MCP server", exception);
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
        // Avoid installing Tomcat's unused WAR/classpath URL handlers as JVM-global state.
        TomcatURLStreamHandlerFactory.disable();
        var tomcat = new Tomcat();
        tomcat.setBaseDir(baseDirectory.toString());
        tomcat.setPort(port);
        Context context = tomcat.addContext("", baseDirectory.toString());
        // Keep Tomcat's packaged cleanup helper visible through NeoForge's module layers.
        context.setParentClassLoader(BlockwiseMcpServer.class.getClassLoader());
        var standardContext = (StandardContext) context;
        // These webapp leak scans are unnecessary and require JVM module opens.
        standardContext.setClearReferencesThreadLocals(false);
        standardContext.setClearReferencesRmiTargets(false);
        var wrapper = context.createWrapper();
        wrapper.setName("mcp");
        wrapper.setServlet(transport);
        wrapper.setLoadOnStartup(1);
        wrapper.setAsyncSupported(true);
        context.addChild(wrapper);
        context.addServletMappingDecoded("/*", "mcp");
        var connector = tomcat.getConnector();
        connector.setProperty("address", HOST);
        connector.setMaxPostSize(1048576);
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
