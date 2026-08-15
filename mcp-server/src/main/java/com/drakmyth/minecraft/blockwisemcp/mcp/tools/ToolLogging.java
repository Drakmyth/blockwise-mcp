package com.drakmyth.minecraft.blockwisemcp.mcp.tools;

import java.util.concurrent.TimeUnit;

final class ToolLogging {
    private static final int MAX_FILTER_LENGTH = 128;

    private ToolLogging() {
    }

    static long elapsedMillis(long startedNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos);
    }

    static String filter(String value) {
        if (value == null) {
            return "<omitted>";
        }

        var escaped = new StringBuilder();
        for (var index = 0; index < value.length(); index++) {
            var character = value.charAt(index);
            switch (character) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (Character.isISOControl(character)) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        if (escaped.length() <= MAX_FILTER_LENGTH) {
            return escaped.toString();
        }
        return escaped.substring(0, MAX_FILTER_LENGTH) + "...";
    }
}
