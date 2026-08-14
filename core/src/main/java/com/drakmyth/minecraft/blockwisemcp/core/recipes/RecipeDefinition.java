package com.drakmyth.minecraft.blockwisemcp.core.recipes;

import java.util.List;
import java.util.Objects;

/** An immutable, statically representable recipe from the supported runtime dataset. */
public record RecipeDefinition(String id, String type, RecipeInput input, List<RecipeOutput> outputs) {
    public RecipeDefinition {
        RecipeIds.requireNamespaced(id, "id");
        RecipeIds.requireNamespaced(type, "type");
        Objects.requireNonNull(input, "input");
        outputs = List.copyOf(Objects.requireNonNull(outputs, "outputs"));
        if (outputs.isEmpty()) {
            throw new IllegalArgumentException("outputs must not be empty");
        }
    }
}
