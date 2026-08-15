package com.drakmyth.minecraft.blockwisemcp.mcp.tools;

import java.util.Map;
import java.util.Objects;

final class ToolArguments {
    private final Map<String, Object> values;

    ToolArguments(Map<String, Object> values) {
        this.values = Objects.requireNonNull(values, "values");
    }

    String requiredString(String name) {
        var value = optionalString(name);
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    String optionalString(String name) {
        var value = values.get(name);
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            return text;
        }
        throw new IllegalArgumentException(name + " must be a string");
    }

    Integer optionalInteger(String name) {
        var value = values.get(name);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            var integer = number.intValue();
            if (number.longValue() == integer && number.doubleValue() == integer) {
                return integer;
            }
        }
        throw new IllegalArgumentException(name + " must be an integer");
    }
}
