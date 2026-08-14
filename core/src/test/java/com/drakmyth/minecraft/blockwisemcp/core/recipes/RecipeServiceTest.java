package com.drakmyth.minecraft.blockwisemcp.core.recipes;

import static com.drakmyth.minecraft.blockwisemcp.core.pagination.InvalidCursorException.Reason.QUERY_MISMATCH;
import static com.drakmyth.minecraft.blockwisemcp.core.pagination.InvalidCursorException.Reason.STALE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.drakmyth.minecraft.blockwisemcp.core.pagination.InvalidCursorException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class RecipeServiceTest {
    private static final String TARGET = "example:target";
    private static final UUID GENERATION = UUID.fromString("3f747dc7-1945-4e9f-96b6-a2543bc88c98");
    private static final UUID OTHER_GENERATION = UUID.fromString("c5667681-59c4-4508-b1b6-ff55e5e01b2c");
    private static final RecipeIngredient INGREDIENT = new RecipeIngredient(List.of("minecraft:coal", "#c:coals"));
    private static final List<RecipeDefinition> RECIPES = List.of(
            recipe("example:zeta", TARGET),
            recipe("example:ignored", "example:other"),
            recipe("example:alpha", TARGET),
            recipe("example:middle", TARGET));

    @Test
    void filtersSortsAndPaginatesByRecipeId() {
        var service = service(new RecipeSnapshot(GENERATION, RECIPES));

        var first = service.findByOutput(new FindRecipesByOutputRequest(TARGET, 2, null));
        var second = service.findByOutput(new FindRecipesByOutputRequest(TARGET, 2, first.nextCursor()));

        assertEquals(List.of("example:alpha", "example:middle"), ids(first.items()));
        assertFalse(first.nextCursor().contains("middle"));
        assertEquals(List.of("example:zeta"), ids(second.items()));
        assertNull(second.nextCursor());
        assertThrows(UnsupportedOperationException.class, () -> first.items().clear());
    }

    @Test
    void validatesRequestAndDataset() {
        var service = service(new RecipeSnapshot(GENERATION, RECIPES));
        assertThrows(NullPointerException.class, () -> service.findByOutput(null));
        assertThrows(NullPointerException.class, () -> new FindRecipesByOutputRequest(null, null, null));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.findByOutput(new FindRecipesByOutputRequest("target", null, null)));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.findByOutput(new FindRecipesByOutputRequest(TARGET, 0, null)));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.findByOutput(new FindRecipesByOutputRequest(TARGET, 101, null)));

        var duplicate = recipe("example:duplicate", TARGET);
        var duplicateService = service(new RecipeSnapshot(GENERATION, List.of(duplicate, duplicate)));
        assertThrows(
                IllegalStateException.class,
                () -> duplicateService.findByOutput(new FindRecipesByOutputRequest(TARGET, null, null)));
    }

    @Test
    void rejectsMismatchedAndStaleCursors() {
        var snapshot = new AtomicReference<>(new RecipeSnapshot(GENERATION, RECIPES));
        var service = new RecipeService(snapshot::get);
        var cursor = service.findByOutput(new FindRecipesByOutputRequest(TARGET, 1, null)).nextCursor();

        assertReason(
                QUERY_MISMATCH,
                () -> service.findByOutput(new FindRecipesByOutputRequest("example:other", 1, cursor)));
        snapshot.set(new RecipeSnapshot(OTHER_GENERATION, RECIPES));
        assertReason(STALE, () -> service.findByOutput(new FindRecipesByOutputRequest(TARGET, 1, cursor)));
    }

    @Test
    void modelsPreserveDeclarativeAndShapedInputsImmutably() {
        var mutableOptions = new ArrayList<>(List.of("minecraft:coal", "#c:coals"));
        var ingredient = new RecipeIngredient(mutableOptions);
        mutableOptions.clear();
        assertEquals(List.of("minecraft:coal", "#c:coals"), ingredient.options());
        assertThrows(IllegalArgumentException.class, () -> new RecipeIngredient(List.of("coals")));

        var row = new ArrayList<RecipeIngredient>();
        row.add(ingredient);
        row.add(null);
        var shaped = new RecipeInput.Shaped(List.of(row));
        row.clear();
        assertEquals(ingredient, shaped.rows().getFirst().get(0));
        assertNull(shaped.rows().getFirst().get(1));
        assertThrows(UnsupportedOperationException.class, () -> shaped.rows().getFirst().clear());
        assertThrows(
                IllegalArgumentException.class,
                () -> new RecipeInput.Shaped(List.of(List.of(ingredient), List.of(ingredient, ingredient))));
    }

    @Test
    void defaultsAndMaximumLimitAreUsable() {
        var service = service(new RecipeSnapshot(GENERATION, RECIPES));
        assertEquals(20, RecipeService.DEFAULT_LIMIT);
        assertEquals(100, RecipeService.MAX_LIMIT);
        assertEquals(3, service.findByOutput(new FindRecipesByOutputRequest(TARGET, 100, null)).items().size());
    }

    private static RecipeService service(RecipeSnapshot snapshot) {
        return new RecipeService(() -> snapshot);
    }

    private static RecipeDefinition recipe(String id, String outputItemId) {
        return new RecipeDefinition(
                id,
                "minecraft:smelting",
                new RecipeInput.Single(INGREDIENT),
                List.of(new RecipeOutput(outputItemId, 1)));
    }

    private static List<String> ids(List<RecipeDefinition> recipes) {
        return recipes.stream().map(RecipeDefinition::id).toList();
    }

    private static void assertReason(InvalidCursorException.Reason reason, Runnable action) {
        var exception = assertThrows(InvalidCursorException.class, action::run);
        assertEquals(reason, exception.reason());
    }
}
