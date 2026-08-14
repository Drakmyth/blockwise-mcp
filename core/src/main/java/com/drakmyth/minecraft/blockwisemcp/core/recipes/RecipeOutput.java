package com.drakmyth.minecraft.blockwisemcp.core.recipes;

import com.drakmyth.minecraft.blockwisemcp.core.ids.ResourceIds;

/** A component-free, statically declared recipe output. */
public record RecipeOutput(String itemId, int count) {
    public RecipeOutput {
        ResourceIds.requireNamespaced(itemId, "itemId");
        if (count < 1) {
            throw new IllegalArgumentException("count must be positive");
        }
    }
}
