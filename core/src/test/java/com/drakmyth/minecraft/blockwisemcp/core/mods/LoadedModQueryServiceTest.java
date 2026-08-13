package com.drakmyth.minecraft.blockwisemcp.core.mods;

import static com.drakmyth.minecraft.blockwisemcp.core.pagination.InvalidCursorException.Reason.MALFORMED;
import static com.drakmyth.minecraft.blockwisemcp.core.pagination.InvalidCursorException.Reason.QUERY_MISMATCH;
import static com.drakmyth.minecraft.blockwisemcp.core.pagination.InvalidCursorException.Reason.STALE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.drakmyth.minecraft.blockwisemcp.core.LoadedMod;
import com.drakmyth.minecraft.blockwisemcp.core.pagination.InvalidCursorException;
import java.util.List;
import org.junit.jupiter.api.Test;

class LoadedModQueryServiceTest {
    private static final long GENERATION = 7;

    private final LoadedModQueryService service = serviceWith(
            new LoadedMod("zeta", "Craft Helper", "1"),
            new LoadedMod("alpha", "Alpha", "2"),
            new LoadedMod("middle", "Middle", "3"));

    @Test
    void filtersIdAndNameCaseInsensitivelyAndSortsById() {
        var page = service.query(new LoadedModQuery("CRAFT", null, null));

        assertEquals(List.of("zeta"), ids(page.items()));
        assertNull(page.nextCursor());
    }

    @Test
    void blankFilterMatchesAllModsInIdOrder() {
        var page = service.query(new LoadedModQuery("  ", null, null));

        assertEquals(List.of("alpha", "middle", "zeta"), ids(page.items()));
    }

    @Test
    void paginatesWithOpaqueCursor() {
        var firstPage = service.query(new LoadedModQuery(null, 2, null));
        var secondPage = service.query(new LoadedModQuery(null, 2, firstPage.nextCursor()));

        assertEquals(List.of("alpha", "middle"), ids(firstPage.items()));
        assertFalse(firstPage.nextCursor().contains("middle"));
        assertEquals(List.of("zeta"), ids(secondPage.items()));
        assertNull(secondPage.nextCursor());
        assertThrows(UnsupportedOperationException.class, () -> firstPage.items().clear());
    }

    @Test
    void rejectsInvalidLimits() {
        assertThrows(IllegalArgumentException.class, () -> service.query(new LoadedModQuery(null, 0, null)));
        assertThrows(IllegalArgumentException.class, () -> service.query(new LoadedModQuery(null, 101, null)));
    }

    @Test
    void rejectsMalformedMismatchedAndStaleCursors() {
        var cursor = service.query(new LoadedModQuery(null, 1, null)).nextCursor();

        assertReason(MALFORMED, () -> service.query(new LoadedModQuery(null, 1, "not base64")));
        assertReason(QUERY_MISMATCH, () -> service.query(new LoadedModQuery("alpha", 1, cursor)));
        assertReason(STALE, () -> serviceWithGeneration(8).query(new LoadedModQuery(null, 1, cursor)));
    }

    @Test
    void defaultAndMaximumLimitsAreUsable() {
        assertEquals(LoadedModQueryService.DEFAULT_LIMIT, 20);
        assertEquals(LoadedModQueryService.MAX_LIMIT, 100);
        assertTrue(service.query(new LoadedModQuery(null, 100, null)).items().size() <= 100);
    }

    private LoadedModQueryService serviceWithGeneration(long generation) {
        return new LoadedModQueryService(serviceSource(), generation);
    }

    private LoadedModQueryService serviceWith(LoadedMod... mods) {
        return new LoadedModQueryService(() -> List.of(mods), GENERATION);
    }

    private com.drakmyth.minecraft.blockwisemcp.core.LoadedModSource serviceSource() {
        return () -> List.of(
                new LoadedMod("zeta", "Craft Helper", "1"),
                new LoadedMod("alpha", "Alpha", "2"),
                new LoadedMod("middle", "Middle", "3"));
    }

    private static List<String> ids(List<LoadedMod> mods) {
        return mods.stream().map(LoadedMod::id).toList();
    }

    private static void assertReason(InvalidCursorException.Reason reason, Runnable action) {
        var exception = assertThrows(InvalidCursorException.class, action::run);
        assertEquals(reason, exception.reason());
    }
}
