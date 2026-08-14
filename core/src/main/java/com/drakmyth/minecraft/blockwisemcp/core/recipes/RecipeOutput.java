package com.drakmyth.minecraft.blockwisemcp.core.recipes;

/** A component-free, statically declared recipe output. */
public record RecipeOutput(String itemId, int count) {
    public RecipeOutput {
        RecipeIds.requireNamespaced(itemId, "itemId");
        if (count < 1) {
            throw new IllegalArgumentException("count must be positive");
        }
    }
}
