package com.drakmyth.minecraft.blockwisemcp.core.recipes;

import com.drakmyth.minecraft.blockwisemcp.core.ids.ResourceId;
import java.util.Objects;

/** A component-free, statically declared recipe output. */
public record RecipeOutput(ResourceId itemId, int count) {
    public RecipeOutput {
        Objects.requireNonNull(itemId, "itemId");
        if (count < 1) {
            throw new IllegalArgumentException("count must be positive");
        }
    }
}
