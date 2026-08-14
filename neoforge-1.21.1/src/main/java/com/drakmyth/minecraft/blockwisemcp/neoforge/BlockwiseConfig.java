package com.drakmyth.minecraft.blockwisemcp.neoforge;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Common configuration for the embedded MCP endpoint. */
public final class BlockwiseConfig {
    public static final int DEFAULT_PORT = 47831;
    public static final int DEFAULT_DISPATCH_TIMEOUT_SECONDS = 5;

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.IntValue PORT;
    public static final ModConfigSpec.IntValue DISPATCH_TIMEOUT_SECONDS;

    static {
        var builder = new ModConfigSpec.Builder();
        ENABLED = builder.comment("Whether to start the MCP endpoint with a Minecraft server.")
                .define("enabled", true);
        PORT = builder.comment("Local TCP port for the MCP endpoint.")
                .defineInRange("port", DEFAULT_PORT, 1, 65535);
        DISPATCH_TIMEOUT_SECONDS = builder.comment("Maximum wait for work dispatched to the server thread.")
                .defineInRange("dispatchTimeoutSeconds", DEFAULT_DISPATCH_TIMEOUT_SECONDS, 1, 60);
        SPEC = builder.build();
    }

    private BlockwiseConfig() {
    }
}
