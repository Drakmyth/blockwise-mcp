package com.drakmyth.minecraft.blockwisemcp;

import com.drakmyth.minecraft.blockwisemcp.neoforge.NeoForgeLoadedModSource;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;

@GameTestHolder(Blockwise.MOD_ID)
public final class BlockwiseGameTests {
    private BlockwiseGameTests() {
    }

    @GameTest(template = "empty")
    public static void initializes(GameTestHelper helper) {
        if (!Blockwise.isInitialized()) {
            helper.fail("Blockwise was not initialized");
            return;
        }

        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void exposesBlockwiseMetadata(GameTestHelper helper) {
        var blockwise = new NeoForgeLoadedModSource().getLoadedMods().stream()
                .filter(mod -> mod.id().equals(Blockwise.MOD_ID))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Blockwise is missing from the loaded mod source"));

        helper.assertValueEqual(blockwise.displayName(), "Blockwise MCP", "display name");
        helper.assertValueEqual(
                blockwise.version(),
                System.getProperty("blockwise.test.expectedVersion"),
                "version");
        helper.succeed();
    }
}
