package com.drakmyth.minecraft.blockwisemcp.core.recipes;

import java.util.regex.Pattern;

final class RecipeIds {
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern PATH = Pattern.compile("[a-z0-9/._-]+");

    private RecipeIds() {
    }

    static String requireNamespaced(String value, String name) {
        if (value == null) {
            throw new NullPointerException(name);
        }
        var separator = value.indexOf(':');
        if (separator < 1
                || separator != value.lastIndexOf(':')
                || !NAMESPACE.matcher(value.substring(0, separator)).matches()
                || !PATH.matcher(value.substring(separator + 1)).matches()) {
            throw new IllegalArgumentException(name + " must be a namespaced resource ID");
        }
        return value;
    }
}
