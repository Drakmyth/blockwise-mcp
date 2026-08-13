package com.drakmyth.minecraft.blockwise;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(Blockwise.MOD_ID)
public final class Blockwise {
    public static final String MOD_ID = "blockwise";

    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean initialized;

    public Blockwise() {
        initialized = true;
        LOGGER.info("Blockwise MCP initialized");
    }

    static boolean isInitialized() {
        return initialized;
    }
}
