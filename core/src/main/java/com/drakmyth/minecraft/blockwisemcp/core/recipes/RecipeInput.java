package com.drakmyth.minecraft.blockwisemcp.core.recipes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Statically representable recipe inputs, discriminated by their concrete format. */
public sealed interface RecipeInput permits RecipeInput.Shaped, RecipeInput.Shapeless, RecipeInput.Single {
    /** A rectangular crafting grid whose nullable cells preserve empty positions. */
    record Shaped(List<List<RecipeIngredient>> rows) implements RecipeInput {
        public Shaped {
            Objects.requireNonNull(rows, "rows");
            if (rows.isEmpty() || rows.getFirst() == null || rows.getFirst().isEmpty()) {
                throw new IllegalArgumentException("rows must contain a nonempty rectangular grid");
            }
            var width = rows.getFirst().size();
            var copy = new ArrayList<List<RecipeIngredient>>(rows.size());
            var hasIngredient = false;
            for (var row : rows) {
                if (row == null || row.size() != width) {
                    throw new IllegalArgumentException("rows must contain a nonempty rectangular grid");
                }
                hasIngredient |= row.stream().anyMatch(Objects::nonNull);
                copy.add(Collections.unmodifiableList(new ArrayList<>(row)));
            }
            if (!hasIngredient) {
                throw new IllegalArgumentException("rows must contain an ingredient");
            }
            rows = List.copyOf(copy);
        }
    }

    /** An unordered, nonempty collection of ingredients. */
    record Shapeless(List<RecipeIngredient> ingredients) implements RecipeInput {
        public Shapeless {
            ingredients = List.copyOf(Objects.requireNonNull(ingredients, "ingredients"));
            if (ingredients.isEmpty()) {
                throw new IllegalArgumentException("ingredients must not be empty");
            }
        }
    }

    /** One ingredient consumed by a cooking or stonecutting recipe. */
    record Single(RecipeIngredient ingredient) implements RecipeInput {
        public Single {
            Objects.requireNonNull(ingredient, "ingredient");
        }
    }
}
