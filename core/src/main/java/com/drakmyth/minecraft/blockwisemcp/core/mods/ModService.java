package com.drakmyth.minecraft.blockwisemcp.core.mods;

import com.drakmyth.minecraft.blockwisemcp.core.mods.LoadedMod;
import com.drakmyth.minecraft.blockwisemcp.core.mods.LoadedModSource;
import com.drakmyth.minecraft.blockwisemcp.core.pagination.CursorCodec;
import com.drakmyth.minecraft.blockwisemcp.core.pagination.InvalidCursorException;
import com.drakmyth.minecraft.blockwisemcp.core.pagination.Page;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class ModService {
    public static final int DEFAULT_LIMIT = 20;
    public static final int MAX_LIMIT = 100;

    private static final int CURSOR_FORMAT_VERSION = 1;

    private final LoadedModSource source;
    private final long generation;
    private final CursorCodec cursorCodec;

    public ModService(LoadedModSource source, long generation) {
        this(source, generation, new CursorCodec());
    }

    ModService(LoadedModSource source, long generation, CursorCodec cursorCodec) {
        this.source = Objects.requireNonNull(source, "source");
        this.generation = generation;
        this.cursorCodec = Objects.requireNonNull(cursorCodec, "cursorCodec");
    }

    public Page<LoadedMod> listLoadedMods(ListLoadedModsRequest request) {
        Objects.requireNonNull(request, "request");
        var filter = normalizeFilter(request.filter());
        var limit = request.limit() == null ? DEFAULT_LIMIT : request.limit();
        validateLimit(limit);
        var position = decodePosition(request.cursor(), filter);

        var matches = source.getLoadedMods().stream()
                .filter(mod -> matches(mod, filter))
                .sorted(Comparator.comparing(LoadedMod::id))
                .filter(mod -> position == null || mod.id().compareTo(position) > 0)
                .limit((long) limit + 1)
                .toList();

        var hasNextPage = matches.size() > limit;
        var items = hasNextPage ? matches.subList(0, limit) : matches;
        var nextCursor = hasNextPage
                ? cursorCodec.encode(CURSOR_FORMAT_VERSION, generation, filter, items.getLast().id())
                : null;
        return new Page<>(items, nextCursor);
    }

    private String decodePosition(String encodedCursor, String filter) {
        if (encodedCursor == null || encodedCursor.isBlank()) {
            return null;
        }
        return cursorCodec.decodePosition(encodedCursor, CURSOR_FORMAT_VERSION, generation, filter);
    }

    private static boolean matches(LoadedMod mod, String filter) {
        return filter.isEmpty()
                || mod.id().toLowerCase(Locale.ROOT).contains(filter)
                || mod.displayName().toLowerCase(Locale.ROOT).contains(filter);
    }

    private static String normalizeFilter(String filter) {
        return filter == null || filter.isBlank() ? "" : filter.toLowerCase(Locale.ROOT);
    }

    private static void validateLimit(int limit) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT);
        }
    }
}

