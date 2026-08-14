package com.drakmyth.minecraft.blockwisemcp.core.ids;

import java.util.regex.Pattern;

/** Validates explicitly namespaced Minecraft resource IDs without depending on Minecraft classes. */
public final class ResourceIds {
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern PATH = Pattern.compile("[a-z0-9/._-]+");

    private ResourceIds() {
    }

    /**
     * Requires a nonempty {@code namespace:path} ID using Minecraft's resource-location character sets.
     *
     * <p>Unlike Minecraft parsing, this contract does not supply the default {@code minecraft} namespace.
     *
     * @param id ID to validate
     * @param name parameter or field name used in failures
     * @return the validated ID unchanged
     * @throws NullPointerException if the ID is null
     * @throws IllegalArgumentException if the ID is not explicitly namespaced or contains invalid characters
     */
    public static String requireNamespaced(String id, String name) {
        if (id == null) {
            throw new NullPointerException(name);
        }
        var separator = id.indexOf(':');
        if (separator < 1
                || separator != id.lastIndexOf(':')
                || !NAMESPACE.matcher(id.substring(0, separator)).matches()
                || !PATH.matcher(id.substring(separator + 1)).matches()) {
            throw new IllegalArgumentException(name + " must be a namespaced resource ID");
        }
        return id;
    }
}
