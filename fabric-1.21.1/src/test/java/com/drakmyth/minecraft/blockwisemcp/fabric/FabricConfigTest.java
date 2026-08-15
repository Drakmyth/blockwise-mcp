package com.drakmyth.minecraft.blockwisemcp.fabric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FabricConfigTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void createsAndLoadsDefaults() throws Exception {
        var path = temporaryDirectory.resolve("config/blockwisemcp.json");

        var config = FabricConfig.load(path);

        assertTrue(config.enabled());
        assertEquals(FabricConfig.DEFAULT_PORT, config.port());
        assertEquals(FabricConfig.DEFAULT_DISPATCH_TIMEOUT_SECONDS, config.dispatchTimeoutSeconds());
        assertTrue(Files.isRegularFile(path));
    }

    @Test
    void loadsConfiguredValues() throws Exception {
        var path = temporaryDirectory.resolve("blockwisemcp.json");
        Files.writeString(path, """
                {
                  "enabled": false,
                  "port": 12345,
                  "dispatchTimeoutSeconds": 12
                }
                """);

        var config = FabricConfig.load(path);

        assertFalse(config.enabled());
        assertEquals(12345, config.port());
        assertEquals(12, config.dispatchTimeoutSeconds());
    }

    @Test
    void rejectsInvalidValues() throws Exception {
        assertInvalid("{\"enabled\": 1}");
        assertInvalid("{\"port\": 0}");
        assertInvalid("{\"port\": 1.5}");
        assertInvalid("{\"dispatchTimeoutSeconds\": 61}");
        assertInvalid("not json");
    }

    private void assertInvalid(String json) throws Exception {
        var path = temporaryDirectory.resolve("invalid.json");
        Files.writeString(path, json);
        assertThrows(IllegalArgumentException.class, () -> FabricConfig.load(path));
    }
}
