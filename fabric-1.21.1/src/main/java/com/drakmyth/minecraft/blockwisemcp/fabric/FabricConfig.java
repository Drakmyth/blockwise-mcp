package com.drakmyth.minecraft.blockwisemcp.fabric;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

record FabricConfig(boolean enabled, int port, int dispatchTimeoutSeconds) {
    static final int DEFAULT_PORT = 47831;
    static final int DEFAULT_DISPATCH_TIMEOUT_SECONDS = 5;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    static FabricConfig load(Path path) throws IOException {
        if (Files.notExists(path)) {
            var parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, GSON.toJson(defaults()) + System.lineSeparator());
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            var defaults = defaults();
            return new FabricConfig(
                    booleanValue(json, "enabled", defaults.enabled()),
                    intValue(json, "port", defaults.port(), 1, 65535),
                    intValue(json, "dispatchTimeoutSeconds", defaults.dispatchTimeoutSeconds(), 1, 60));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid Blockwise MCP Fabric configuration: " + path, exception);
        }
    }

    static FabricConfig defaults() {
        return new FabricConfig(true, DEFAULT_PORT, DEFAULT_DISPATCH_TIMEOUT_SECONDS);
    }

    static FabricConfig disabled() {
        return new FabricConfig(false, DEFAULT_PORT, DEFAULT_DISPATCH_TIMEOUT_SECONDS);
    }

    Duration dispatchTimeout() {
        return Duration.ofSeconds(dispatchTimeoutSeconds);
    }

    private static boolean booleanValue(JsonObject json, String name, boolean defaultValue) {
        if (!json.has(name)) {
            return defaultValue;
        }
        var value = json.get(name);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
            throw new IllegalArgumentException(name + " must be a boolean");
        }
        return value.getAsBoolean();
    }

    private static int intValue(JsonObject json, String name, int defaultValue, int minimum, int maximum) {
        if (!json.has(name)) {
            return defaultValue;
        }
        var value = json.get(name);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException(name + " must be an integer");
        }
        int number = value.getAsBigDecimal().intValueExact();
        if (number < minimum || number > maximum) {
            throw new IllegalArgumentException(name + " must be between " + minimum + " and " + maximum);
        }
        return number;
    }
}
