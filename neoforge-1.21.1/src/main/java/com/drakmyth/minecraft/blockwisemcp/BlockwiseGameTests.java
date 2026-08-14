package com.drakmyth.minecraft.blockwisemcp;

import com.drakmyth.minecraft.blockwisemcp.core.ids.ResourceId;
import com.drakmyth.minecraft.blockwisemcp.core.recipes.IngredientOption;
import com.drakmyth.minecraft.blockwisemcp.core.recipes.RecipeDefinition;
import com.drakmyth.minecraft.blockwisemcp.core.recipes.RecipeInput;
import com.drakmyth.minecraft.blockwisemcp.neoforge.NeoForgeLoadedModSource;
import com.drakmyth.minecraft.blockwisemcp.neoforge.NeoForgeRecipeSource;
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
    public static void startsMcpEndpoint(GameTestHelper helper) {
        if (!Blockwise.isMcpRunning()) {
            helper.fail("Blockwise MCP endpoint is not running");
            return;
        }

        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void exposesSupportedRecipes(GameTestHelper helper) {
        var source = new NeoForgeRecipeSource(helper.getLevel().getServer());
        var recipes = source.getRecipes().recipes();

        var shaped = recipe(recipes, "minecraft:crafting_table");
        helper.assertValueEqual(shaped.type().toString(), "minecraft:crafting_shaped", "shaped serializer");
        helper.assertTrue(shaped.input() instanceof RecipeInput.Shaped, "crafting table input should be shaped");
        var rows = ((RecipeInput.Shaped) shaped.input()).rows();
        helper.assertValueEqual(rows.size(), 2, "shaped height");
        helper.assertValueEqual(rows.getFirst().size(), 2, "shaped width");
        var planks = rows.getFirst().getFirst().options().getFirst();
        helper.assertTrue(planks instanceof IngredientOption.Tag, "crafting table should preserve the planks tag");
        helper.assertValueEqual(planks.selector(), "#minecraft:planks", "tag selector");
        helper.assertValueEqual(shaped.outputs().getFirst().itemId().toString(), "minecraft:crafting_table", "output item");
        helper.assertValueEqual(shaped.outputs().getFirst().count(), 1, "output count");

        helper.assertTrue(
                recipe(recipes, "minecraft:flint_and_steel").input() instanceof RecipeInput.Shapeless,
                "flint and steel input should be shapeless");
        helper.assertTrue(
                recipe(recipes, "minecraft:iron_ingot_from_smelting_iron_ore").input() instanceof RecipeInput.Single,
                "smelting input should be single");
        helper.assertTrue(
                recipe(recipes, "minecraft:stone_bricks_from_stone_stonecutting").input() instanceof RecipeInput.Single,
                "stonecutting input should be single");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void excludesUnsupportedRecipesAndAdvancesGeneration(GameTestHelper helper) {
        var source = new NeoForgeRecipeSource(helper.getLevel().getServer());
        var first = source.getRecipes();
        helper.assertTrue(
                first.recipes().stream().noneMatch(recipe -> recipe.id().equals(ResourceId.parse("minecraft:armor_dye"))),
                "special recipes should be excluded");
        helper.assertTrue(
                first.recipes().stream().noneMatch(recipe -> recipe.id().equals(ResourceId.parse("minecraft:netherite_sword_smithing"))),
                "smithing recipes should be excluded");

        source.advanceGeneration();
        helper.assertFalse(first.generation().equals(source.getRecipes().generation()), "generation should advance");
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

    private static RecipeDefinition recipe(java.util.List<RecipeDefinition> recipes, String id) {
        var resourceId = ResourceId.parse(id);
        return recipes.stream()
                .filter(recipe -> recipe.id().equals(resourceId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Recipe source is missing " + id));
    }
}
