package com.drakmyth.minecraft.blockwisemcp.neoforge;

import java.time.Duration;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Common configuration for the embedded MCP endpoint. */
public final class NeoForgeConfig {
    public static final int DEFAULT_PORT = 47831;
    public static final int DEFAULT_DISPATCH_TIMEOUT_SECONDS = 5;

    private final ModConfigSpec spec;
    private final ModConfigSpec.BooleanValue enabled;
    private final ModConfigSpec.IntValue port;
    private final ModConfigSpec.IntValue dispatchTimeoutSeconds;

    public NeoForgeConfig() {
        var builder = new ModConfigSpec.Builder();
        enabled = builder.comment("Whether to start the MCP endpoint with a Minecraft server.")
                .define("enabled", true);
        port = builder.comment("Local TCP port for the MCP endpoint.")
                .defineInRange("port", DEFAULT_PORT, 1, 65535);
        dispatchTimeoutSeconds = builder.comment("Maximum wait for work dispatched to the server thread.")
                .defineInRange("dispatchTimeoutSeconds", DEFAULT_DISPATCH_TIMEOUT_SECONDS, 1, 60);
        spec = builder.build();
    }

    public ModConfigSpec spec() {
        return spec;
    }

    public boolean enabled() {
        return enabled.get();
    }

    public int port() {
        return port.get();
    }

    public Duration dispatchTimeout() {
        return Duration.ofSeconds(dispatchTimeoutSeconds.get());
    }
}
