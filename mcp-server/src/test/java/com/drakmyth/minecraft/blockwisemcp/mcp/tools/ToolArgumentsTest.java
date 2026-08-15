package com.drakmyth.minecraft.blockwisemcp.mcp.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ToolArgumentsTest {
    @Test
    void readsRequiredAndOptionalValues() {
        var arguments = new ToolArguments(Map.of("required", "value", "limit", 20L));

        assertEquals("value", arguments.requiredString("required"));
        assertEquals(20, arguments.optionalInteger("limit"));
        assertNull(arguments.optionalString("omitted"));
    }

    @Test
    void rejectsMissingRequiredString() {
        var arguments = new ToolArguments(Map.of());

        assertEquals(
                "required is required",
                assertThrows(IllegalArgumentException.class, () -> arguments.requiredString("required")).getMessage());
    }

    @Test
    void rejectsWrongTypesAndOutOfRangeIntegers() {
        var wrongString = new ToolArguments(Map.of("value", 1));
        var decimal = new ToolArguments(Map.of("value", 1.5));
        var tooLarge = new ToolArguments(Map.of("value", (long) Integer.MAX_VALUE + 1));

        assertThrows(IllegalArgumentException.class, () -> wrongString.optionalString("value"));
        assertThrows(IllegalArgumentException.class, () -> decimal.optionalInteger("value"));
        assertThrows(IllegalArgumentException.class, () -> tooLarge.optionalInteger("value"));
    }
}
