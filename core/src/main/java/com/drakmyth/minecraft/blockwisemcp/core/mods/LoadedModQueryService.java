package com.drakmyth.minecraft.blockwisemcp.core.mods;

import static com.drakmyth.minecraft.blockwisemcp.core.pagination.InvalidCursorException.Reason.QUERY_MISMATCH;
import static com.drakmyth.minecraft.blockwisemcp.core.pagination.InvalidCursorException.Reason.STALE;
import static com.drakmyth.minecraft.blockwisemcp.core.pagination.InvalidCursorException.Reason.UNSUPPORTED_FORMAT;

import com.drakmyth.minecraft.blockwisemcp.core.LoadedMod;
import com.drakmyth.minecraft.blockwisemcp.core.LoadedModSource;
import com.drakmyth.minecraft.blockwisemcp.core.pagination.Cursor;
import com.drakmyth.minecraft.blockwisemcp.core.pagination.CursorCodec;
import com.drakmyth.minecraft.blockwisemcp.core.pagination.InvalidCursorException;
import com.drakmyth.minecraft.blockwisemcp.core.pagination.Page;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class LoadedModQueryService {
    public static final int DEFAULT_LIMIT = 20;
    public static final int MAX_LIMIT = 100;

    private static final int CURSOR_FORMAT_VERSION = 1;

    private final LoadedModSource source;
    private final long generation;
    private final CursorCodec cursorCodec;

    public LoadedModQueryService(LoadedModSource source, long generation) {
        this(source, generation, new CursorCodec());
    }

    LoadedModQueryService(LoadedModSource source, long generation, CursorCodec cursorCodec) {
        this.source = Objects.requireNonNull(source, "source");
        this.generation = generation;
        this.cursorCodec = Objects.requireNonNull(cursorCodec, "cursorCodec");
    }

    public Page<LoadedMod> query(LoadedModQuery query) {
        Objects.requireNonNull(query, "query");
        var filter = normalizeFilter(query.filter());
        var limit = query.limit() == null ? DEFAULT_LIMIT : query.limit();
        validateLimit(limit);
        var position = decodePosition(query.cursor(), filter);

        var matches = source.getLoadedMods().stream()
                .filter(mod -> matches(mod, filter))
                .sorted(Comparator.comparing(LoadedMod::id))
                .filter(mod -> position == null || mod.id().compareTo(position) > 0)
                .limit((long) limit + 1)
                .toList();

        var hasNextPage = matches.size() > limit;
        var items = hasNextPage ? matches.subList(0, limit) : matches;
        var nextCursor = hasNextPage
                ? cursorCodec.encode(new Cursor(CURSOR_FORMAT_VERSION, generation, filter, items.getLast().id()))
                : null;
        return new Page<>(items, nextCursor);
    }

    private String decodePosition(String encodedCursor, String filter) {
        if (encodedCursor == null || encodedCursor.isBlank()) {
            return null;
        }
        var cursor = cursorCodec.decode(encodedCursor);
        if (cursor.formatVersion() != CURSOR_FORMAT_VERSION) {
            throw invalid(UNSUPPORTED_FORMAT, "Cursor format is unsupported");
        }
        if (cursor.generation() != generation) {
            throw invalid(STALE, "Cursor is stale");
        }
        if (!cursor.queryIdentity().equals(filter)) {
            throw invalid(QUERY_MISMATCH, "Cursor does not match the query");
        }
        return cursor.position();
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

    private static InvalidCursorException invalid(InvalidCursorException.Reason reason, String message) {
        return new InvalidCursorException(reason, message);
    }
}

