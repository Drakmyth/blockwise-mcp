package com.drakmyth.minecraft.blockwisemcp.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class LoadedModTest {
    @Test
    void preservesReportedValues() {
        var mod = new LoadedMod("", " Display Name ", "custom version");

        assertEquals("", mod.id());
        assertEquals(" Display Name ", mod.displayName());
        assertEquals("custom version", mod.version());
    }

    @Test
    void rejectsNullValues() {
        assertThrows(NullPointerException.class, () -> new LoadedMod(null, "name", "version"));
        assertThrows(NullPointerException.class, () -> new LoadedMod("id", null, "version"));
        assertThrows(NullPointerException.class, () -> new LoadedMod("id", "name", null));
    }
}
