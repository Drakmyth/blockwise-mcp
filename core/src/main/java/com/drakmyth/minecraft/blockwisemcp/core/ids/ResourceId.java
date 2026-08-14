package com.drakmyth.minecraft.blockwisemcp.core.ids;

import java.util.Objects;
import java.util.regex.Pattern;

/** An explicitly namespaced Minecraft resource ID independent of Minecraft runtime classes. */
public record ResourceId(String namespace, String path) implements Comparable<ResourceId> {
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern PATH = Pattern.compile("[a-z0-9/._-]+");

    public ResourceId {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(path, "path");
        if (!NAMESPACE.matcher(namespace).matches() || !PATH.matcher(path).matches()) {
            throw new IllegalArgumentException("namespace and path must form a valid nonempty resource ID");
        }
    }

    /** Parses an explicit nonempty {@code namespace:path} ID without supplying a default namespace. */
    public static ResourceId parse(String id) {
        Objects.requireNonNull(id, "id");
        var separator = id.indexOf(':');
        if (separator < 1 || separator != id.lastIndexOf(':')) {
            throw new IllegalArgumentException("id must be an explicitly namespaced resource ID");
        }
        return new ResourceId(id.substring(0, separator), id.substring(separator + 1));
    }

    @Override
    public int compareTo(ResourceId other) {
        return toString().compareTo(other.toString());
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }
}
