package com.drakmyth.minecraft.blockwisemcp.core.mods;

import static com.drakmyth.minecraft.blockwisemcp.core.pagination.InvalidCursorException.Reason.MALFORMED;
import static com.drakmyth.minecraft.blockwisemcp.core.pagination.InvalidCursorException.Reason.QUERY_MISMATCH;
import static com.drakmyth.minecraft.blockwisemcp.core.pagination.InvalidCursorException.Reason.STALE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.drakmyth.minecraft.blockwisemcp.core.pagination.InvalidCursorException;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModServiceTest {
    private static final long GENERATION = 7;
    private static final List<LoadedMod> MODS = List.of(
            new LoadedMod("zeta", "Craft Helper", "1"),
            new LoadedMod("alpha", "Alpha", "2"),
            new LoadedMod("middle", "Middle", "3"));

    private final ModService service = serviceWithGeneration(GENERATION);

    @Test
    void filtersIdAndNameCaseInsensitivelyAndSortsById() {
        var page = service.listLoadedMods(new ListLoadedModsRequest("CRAFT", null, null));

        assertEquals(List.of("zeta"), ids(page.items()));
        assertNull(page.nextCursor());
    }

    @Test
    void blankFilterMatchesAllModsInIdOrder() {
        var page = service.listLoadedMods(new ListLoadedModsRequest("  ", null, null));

        assertEquals(List.of("alpha", "middle", "zeta"), ids(page.items()));
    }

    @Test
    void paginatesWithOpaqueCursor() {
        var firstPage = service.listLoadedMods(new ListLoadedModsRequest(null, 2, null));
        var secondPage = service.listLoadedMods(new ListLoadedModsRequest(null, 2, firstPage.nextCursor()));

        assertEquals(List.of("alpha", "middle"), ids(firstPage.items()));
        assertFalse(firstPage.nextCursor().contains("middle"));
        assertEquals(List.of("zeta"), ids(secondPage.items()));
        assertNull(secondPage.nextCursor());
        assertThrows(UnsupportedOperationException.class, () -> firstPage.items().clear());
    }

    @Test
    void rejectsInvalidLimits() {
        assertThrows(IllegalArgumentException.class, () -> service.listLoadedMods(new ListLoadedModsRequest(null, 0, null)));
        assertThrows(IllegalArgumentException.class, () -> service.listLoadedMods(new ListLoadedModsRequest(null, 101, null)));
    }

    @Test
    void rejectsMalformedMismatchedAndStaleCursors() {
        var cursor = service.listLoadedMods(new ListLoadedModsRequest(null, 1, null)).nextCursor();

        assertReason(MALFORMED, () -> service.listLoadedMods(new ListLoadedModsRequest(null, 1, "not base64")));
        assertReason(QUERY_MISMATCH, () -> service.listLoadedMods(new ListLoadedModsRequest("alpha", 1, cursor)));
        assertReason(STALE, () -> serviceWithGeneration(8).listLoadedMods(new ListLoadedModsRequest(null, 1, cursor)));
    }

    @Test
    void defaultAndMaximumLimitsAreUsable() {
        assertEquals(ModService.DEFAULT_LIMIT, 20);
        assertEquals(ModService.MAX_LIMIT, 100);
        assertTrue(service.listLoadedMods(new ListLoadedModsRequest(null, 100, null)).items().size() <= 100);
    }

    private ModService serviceWithGeneration(long generation) {
        return new ModService(() -> MODS, generation);
    }

    private static List<String> ids(List<LoadedMod> mods) {
        return mods.stream().map(LoadedMod::id).toList();
    }

    private static void assertReason(InvalidCursorException.Reason reason, Runnable action) {
        var exception = assertThrows(InvalidCursorException.class, action::run);
        assertEquals(reason, exception.reason());
    }
}
