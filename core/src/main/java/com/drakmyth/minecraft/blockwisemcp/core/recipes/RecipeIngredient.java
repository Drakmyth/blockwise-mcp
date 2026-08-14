package com.drakmyth.minecraft.blockwisemcp.core.recipes;

import com.drakmyth.minecraft.blockwisemcp.core.ids.ResourceIds;
import java.util.List;
import java.util.Objects;

/** An ingredient satisfied by any exact item ID or {@code #}-prefixed item tag in {@link #options()}. */
public record RecipeIngredient(List<String> options) {
    public RecipeIngredient {
        options = List.copyOf(Objects.requireNonNull(options, "options"));
        if (options.isEmpty()) {
            throw new IllegalArgumentException("options must not be empty");
        }
        for (var option : options) {
            var id = option != null && option.startsWith("#") ? option.substring(1) : option;
            ResourceIds.requireNamespaced(id, "option");
        }
    }
}
