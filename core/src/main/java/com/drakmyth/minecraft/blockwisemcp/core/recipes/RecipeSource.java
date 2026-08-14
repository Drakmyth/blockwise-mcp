package com.drakmyth.minecraft.blockwisemcp.core.recipes;

/** Supplies the current authoritative, statically supported recipe dataset. */
@FunctionalInterface
public interface RecipeSource {
    RecipeSnapshot getRecipes();
}
