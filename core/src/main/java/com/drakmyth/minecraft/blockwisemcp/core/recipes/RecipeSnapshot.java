package com.drakmyth.minecraft.blockwisemcp.core.recipes;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** A coherent recipe dataset and its opaque server-session or reload generation. */
public record RecipeSnapshot(UUID generation, List<RecipeDefinition> recipes) {
    public RecipeSnapshot {
        Objects.requireNonNull(generation, "generation");
        recipes = List.copyOf(Objects.requireNonNull(recipes, "recipes"));
    }
}
