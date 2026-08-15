package com.drakmyth.minecraft.blockwisemcp.neoforge;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.electronwill.nightconfig.core.CommentedConfig;
import org.junit.jupiter.api.Test;

class NeoForgeConfigTest {
    @Test
    void definesDefaultsAndCorrectsOutOfRangeValues() {
        var config = new NeoForgeConfig();
        var values = CommentedConfig.inMemory();

        config.spec().correct(values);

        assertEquals(true, values.<Boolean>get("enabled"));
        assertEquals(NeoForgeConfig.DEFAULT_PORT, values.<Integer>get("port"));
        assertEquals(
                NeoForgeConfig.DEFAULT_DISPATCH_TIMEOUT_SECONDS,
                values.<Integer>get("dispatchTimeoutSeconds"));

        values.set("port", 0);
        values.set("dispatchTimeoutSeconds", 61);
        config.spec().correct(values);

        assertEquals(1, values.<Integer>get("port"));
        assertEquals(60, values.<Integer>get("dispatchTimeoutSeconds"));
    }
}
