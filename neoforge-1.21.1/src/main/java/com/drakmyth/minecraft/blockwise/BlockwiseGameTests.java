package com.drakmyth.minecraft.blockwise;

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
}
