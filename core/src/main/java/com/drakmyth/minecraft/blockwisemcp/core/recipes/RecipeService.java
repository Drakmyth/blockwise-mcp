package com.drakmyth.minecraft.blockwisemcp.core.recipes;

import com.drakmyth.minecraft.blockwisemcp.core.pagination.CursorCodec;
import com.drakmyth.minecraft.blockwisemcp.core.pagination.Page;
import java.util.Comparator;
import java.util.Objects;

/** Provides loader-independent output lookup over supported runtime recipes. */
public final class RecipeService {
    public static final int DEFAULT_LIMIT = 20;
    public static final int MAX_LIMIT = 100;

    private static final int CURSOR_FORMAT_VERSION = 1;

    private final RecipeSource source;
    private final CursorCodec cursorCodec;

    public RecipeService(RecipeSource source) {
        this(source, new CursorCodec());
    }

    RecipeService(RecipeSource source, CursorCodec cursorCodec) {
        this.source = Objects.requireNonNull(source, "source");
        this.cursorCodec = Objects.requireNonNull(cursorCodec, "cursorCodec");
    }

    /**
     * Finds recipes with a declared output matching the requested exact item ID.
     *
     * @throws NullPointerException if {@code request} is null
     * @throws IllegalArgumentException if the item ID, limit, or cursor is invalid
     * @throws IllegalStateException if the source contains duplicate recipe IDs
     */
    public Page<RecipeDefinition> findByOutput(FindRecipesByOutputRequest request) {
        Objects.requireNonNull(request, "request");
        var outputItemId = RecipeIds.requireNamespaced(request.outputItemId(), "outputItemId");
        var limit = request.limit() == null ? DEFAULT_LIMIT : request.limit();
        validateLimit(limit);
        var snapshot = Objects.requireNonNull(source.getRecipes(), "source result");
        var position = decodePosition(request.cursor(), snapshot, outputItemId);

        var matches = snapshot.recipes().stream()
                .sorted(Comparator.comparing(RecipeDefinition::id))
                .toList();
        for (var index = 1; index < matches.size(); index++) {
            if (matches.get(index - 1).id().equals(matches.get(index).id())) {
                throw new IllegalStateException("duplicate recipe ID: " + matches.get(index).id());
            }
        }
        matches = matches.stream()
                .filter(recipe -> recipe.outputs().stream().anyMatch(output -> output.itemId().equals(outputItemId)))
                .filter(recipe -> position == null || recipe.id().compareTo(position) > 0)
                .limit((long) limit + 1)
                .toList();

        var hasNextPage = matches.size() > limit;
        var items = hasNextPage ? matches.subList(0, limit) : matches;
        var nextCursor = hasNextPage
                ? cursorCodec.encode(
                        CURSOR_FORMAT_VERSION, snapshot.generation(), outputItemId, items.getLast().id())
                : null;
        return new Page<>(items, nextCursor);
    }

    private String decodePosition(String cursor, RecipeSnapshot snapshot, String outputItemId) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        return cursorCodec.decodePosition(cursor, CURSOR_FORMAT_VERSION, snapshot.generation(), outputItemId);
    }

    private static void validateLimit(int limit) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT);
        }
    }
}
