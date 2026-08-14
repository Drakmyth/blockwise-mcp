package com.drakmyth.minecraft.blockwisemcp.core.recipes;

import com.drakmyth.minecraft.blockwisemcp.core.ids.ResourceId;
import java.util.Objects;

/** One exact item or item-tag alternative in a recipe ingredient. */
public sealed interface IngredientOption permits IngredientOption.Item, IngredientOption.Tag {
    ResourceId id();

    /** Returns the canonical item ID or {@code #}-prefixed item-tag selector. */
    String selector();

    /** Parses an item ID or a {@code #}-prefixed item-tag ID. */
    static IngredientOption parse(String value) {
        Objects.requireNonNull(value, "value");
        return value.startsWith("#")
                ? new Tag(ResourceId.parse(value.substring(1)))
                : new Item(ResourceId.parse(value));
    }

    record Item(ResourceId id) implements IngredientOption {
        public Item {
            Objects.requireNonNull(id, "id");
        }

        @Override
        public String selector() {
            return id.toString();
        }
    }

    record Tag(ResourceId id) implements IngredientOption {
        public Tag {
            Objects.requireNonNull(id, "id");
        }

        @Override
        public String selector() {
            return "#" + id.toString();
        }
    }
}
