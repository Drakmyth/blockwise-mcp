package com.drakmyth.minecraft.blockwisemcp.core.recipes;

import com.drakmyth.minecraft.blockwisemcp.core.ids.ResourceId;
import java.util.List;
import java.util.Objects;

/** An immutable, statically representable recipe from the supported runtime dataset. */
public record RecipeDefinition(ResourceId id, ResourceId type, RecipeInput input, List<RecipeOutput> outputs) {
    public RecipeDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(input, "input");
        outputs = List.copyOf(Objects.requireNonNull(outputs, "outputs"));
        if (outputs.isEmpty()) {
            throw new IllegalArgumentException("outputs must not be empty");
        }
    }
}
