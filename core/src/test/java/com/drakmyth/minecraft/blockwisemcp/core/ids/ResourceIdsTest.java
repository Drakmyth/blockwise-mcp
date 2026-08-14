package com.drakmyth.minecraft.blockwisemcp.core.ids;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ResourceIdsTest {
    @Test
    void acceptsMinecraftResourceLocationCharacters() {
        assertEquals(
                "example.namespace:path/to_value-1.2",
                ResourceIds.requireNamespaced("example.namespace:path/to_value-1.2", "id"));
    }

    @Test
    void requiresExplicitNonemptyNamespaceAndPath() {
        assertInvalid("path");
        assertInvalid(":path");
        assertInvalid("namespace:");
        assertInvalid("namespace:path:extra");
    }

    @Test
    void rejectsCharactersOutsideMinecraftResourceLocations() {
        assertInvalid("Example:path");
        assertInvalid("example:Path");
        assertInvalid("example:path value");
        assertInvalid("example:path#fragment");
        assertThrows(NullPointerException.class, () -> ResourceIds.requireNamespaced(null, "id"));
    }

    private static void assertInvalid(String id) {
        assertThrows(IllegalArgumentException.class, () -> ResourceIds.requireNamespaced(id, "id"));
    }
}
