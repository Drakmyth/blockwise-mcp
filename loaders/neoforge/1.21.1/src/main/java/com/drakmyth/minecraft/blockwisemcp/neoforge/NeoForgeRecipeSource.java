package com.drakmyth.minecraft.blockwisemcp.neoforge;

import com.drakmyth.minecraft.blockwisemcp.core.ids.ResourceId;
import com.drakmyth.minecraft.blockwisemcp.core.recipes.IngredientOption;
import com.drakmyth.minecraft.blockwisemcp.core.recipes.RecipeDefinition;
import com.drakmyth.minecraft.blockwisemcp.core.recipes.RecipeIngredient;
import com.drakmyth.minecraft.blockwisemcp.core.recipes.RecipeInput;
import com.drakmyth.minecraft.blockwisemcp.core.recipes.RecipeOutput;
import com.drakmyth.minecraft.blockwisemcp.core.recipes.RecipeSnapshot;
import com.drakmyth.minecraft.blockwisemcp.core.recipes.RecipeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;

/** Reads statically representable recipes from the active NeoForge server. */
public final class NeoForgeRecipeSource implements RecipeSource {
    private final MinecraftServer server;
    private volatile UUID generation = UUID.randomUUID();

    public NeoForgeRecipeSource(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    public RecipeSnapshot getRecipes() {
        var definitions = server.getRecipeManager().getRecipes().stream()
                .map(this::mapRecipe)
                .filter(Objects::nonNull)
                .toList();
        return new RecipeSnapshot(generation, definitions);
    }

    /** Invalidates cursors after a successful server-data reload. */
    public void advanceGeneration() {
        generation = UUID.randomUUID();
    }

    private RecipeDefinition mapRecipe(RecipeHolder<?> holder) {
        var recipe = holder.value();
        var input = mapInput(recipe);
        if (input == null) {
            return null;
        }

        var output = recipe.getResultItem(server.registryAccess());
        if (output.isEmpty() || !output.getComponentsPatch().isEmpty()) {
            return null;
        }

        var serializerId = BuiltInRegistries.RECIPE_SERIALIZER.getKey(recipe.getSerializer());
        if (serializerId == null) {
            throw new IllegalStateException("Supported recipe has an unregistered serializer: " + holder.id());
        }

        return new RecipeDefinition(
                resourceId(holder.id()),
                resourceId(serializerId),
                input,
                List.of(new RecipeOutput(itemId(output), output.getCount())));
    }

    private RecipeInput mapInput(Recipe<?> recipe) {
        if (recipe instanceof ShapedRecipe shaped) {
            var ingredients = shaped.getIngredients();
            var rows = new ArrayList<List<RecipeIngredient>>(shaped.getHeight());
            for (var rowIndex = 0; rowIndex < shaped.getHeight(); rowIndex++) {
                var row = new ArrayList<RecipeIngredient>(shaped.getWidth());
                for (var columnIndex = 0; columnIndex < shaped.getWidth(); columnIndex++) {
                    var ingredient = ingredients.get(rowIndex * shaped.getWidth() + columnIndex);
                    var mapped = mapIngredient(ingredient);
                    if (!ingredient.isEmpty() && mapped == null) {
                        return null;
                    }
                    row.add(mapped);
                }
                rows.add(row);
            }
            return new RecipeInput.Shaped(rows);
        }
        if (recipe instanceof ShapelessRecipe shapeless) {
            return mapShapeless(shapeless.getIngredients());
        }
        if (recipe instanceof AbstractCookingRecipe cooking) {
            return mapSingle(cooking.getIngredients());
        }
        if (recipe instanceof StonecutterRecipe stonecutting) {
            return mapSingle(stonecutting.getIngredients());
        }
        return null;
    }

    private RecipeInput mapShapeless(List<Ingredient> ingredients) {
        var mapped = new ArrayList<RecipeIngredient>(ingredients.size());
        for (var ingredient : ingredients) {
            var definition = mapIngredient(ingredient);
            if (definition == null) {
                return null;
            }
            mapped.add(definition);
        }
        return new RecipeInput.Shapeless(mapped);
    }

    private RecipeInput mapSingle(List<Ingredient> ingredients) {
        if (ingredients.size() != 1) {
            throw new IllegalStateException("Supported single-input recipe did not expose exactly one ingredient");
        }
        var ingredient = mapIngredient(ingredients.getFirst());
        return ingredient == null ? null : new RecipeInput.Single(ingredient);
    }

    private RecipeIngredient mapIngredient(Ingredient ingredient) {
        if (ingredient.isEmpty() || ingredient.isCustom()) {
            return null;
        }

        var options = new ArrayList<IngredientOption>();
        for (var value : ingredient.getValues()) {
            if (value instanceof Ingredient.ItemValue itemValue) {
                if (!itemValue.item().getComponentsPatch().isEmpty()) {
                    return null;
                }
                options.add(new IngredientOption.Item(itemId(itemValue.item())));
            } else if (value instanceof Ingredient.TagValue tagValue) {
                options.add(new IngredientOption.Tag(resourceId(tagValue.tag().location())));
            } else {
                throw new IllegalStateException("Supported ingredient exposed an unknown value type: " + value.getClass().getName());
            }
        }
        return options.isEmpty() ? null : new RecipeIngredient(options);
    }

    private static ResourceId itemId(ItemStack stack) {
        var location = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (location == null) {
            throw new IllegalStateException("Recipe references an unregistered item");
        }
        return resourceId(location);
    }

    private static ResourceId resourceId(ResourceLocation location) {
        return new ResourceId(location.getNamespace(), location.getPath());
    }
}
