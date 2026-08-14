package com.drakmyth.minecraft.blockwisemcp.core.ids;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ResourceIdTest {
    @Test
    void parsesMinecraftResourceLocationCharacters() {
        var id = ResourceId.parse("example.namespace:path/to_value-1.2");

        assertEquals("example.namespace", id.namespace());
        assertEquals("path/to_value-1.2", id.path());
        assertEquals("example.namespace:path/to_value-1.2", id.toString());
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
        assertThrows(NullPointerException.class, () -> ResourceId.parse(null));
    }

    @Test
    void comparesByCanonicalId() {
        var alpha = ResourceId.parse("alpha:zeta");
        var zeta = ResourceId.parse("zeta:alpha");

        assertTrue(alpha.compareTo(zeta) < 0);
    }

    private static void assertInvalid(String id) {
        assertThrows(IllegalArgumentException.class, () -> ResourceId.parse(id));
    }
}
