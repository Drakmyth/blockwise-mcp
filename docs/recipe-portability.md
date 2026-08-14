# Recipe Portability Reconnaissance

This document compares the recipe surfaces available to Blockwise MCP on NeoForge and Fabric for Minecraft 1.21.1. It informs contract design; it does not commit the project to a Fabric implementation.

## Scope

Compared environments:

- NeoForge 21.1.1 with official Mojang mappings
- Fabric Loader 0.15.11 and Fabric API 0.116.15+1.21.1
- Minecraft 1.21.1 and Java 21 in both environments

The Fabric API was inspected at tag `0.116.15+1.21.1`. A disposable Fabric Loom probe using official Mojang mappings compiled recipe enumeration and lifecycle callbacks. No Fabric production module was added.

Initial Blockwise support remains limited to recipes registered under Minecraft's built-in recipe types. A mod namespace does not exclude a recipe. Custom recipe and ingredient mechanisms were inspected only to preserve future extension paths.

## Shared Minecraft surface

Both loaders expose the same central Minecraft model:

- `MinecraftServer.getRecipeManager()` provides the authoritative server recipe manager.
- `RecipeManager.getRecipes()` enumerates all loaded `RecipeHolder<?>` values.
- `RecipeHolder` provides the recipe ID and recipe value.
- `Recipe.getType()` and the recipe-type registry identify the registered type.
- `Recipe.getResultItem(HolderLookup.Provider)` provides the declared result stack.
- `Recipe.getIngredients()` provides generic ingredients where the implementation supports it.
- `ItemStack` carries the result item, count, and data components.

Minecraft 1.21.1 declares these built-in recipe types:

- crafting
- smelting
- blasting
- smoking
- campfire cooking
- stonecutting
- smithing

The shared API supports one loader-neutral source boundary. `core` does not need loader concepts to represent recipe IDs, type IDs, ingredients, outputs, ordering, or pagination.

## Built-in recipe limitations

The common interface does not guarantee complete static introspection:

- Shaped, shapeless, cooking, and single-item recipes expose generic ingredients and fixed declared results.
- Shaped recipes additionally expose width and height.
- Special crafting recipes may compute outputs from runtime inputs. `isSpecial()` is a useful signal but is not a complete declaration of output stability.
- Smithing trim output depends on the input stack and its components.
- Smithing transform has a declared result, but its template, base, and addition fields have no public generic ingredient accessors in Minecraft 1.21.1.
- A custom implementation of a built-in type can technically return incomplete or unconventional generic data.

Consequently, registration under a built-in type is necessary but not sufficient. The adapter must also establish that output and input semantics are statically representable. Unsupported recipes should be omitted or reported explicitly according to the final contract; Blockwise must not manufacture lossy data and present it as authoritative.

## Loader differences

### Recipe registration and conditions

Both loaders ultimately register custom `RecipeType` and `RecipeSerializer` values in Minecraft registries. Their registration conventions differ:

- NeoForge commonly uses deferred registries and adds conditional recipe decoding.
- Fabric commonly registers directly through Minecraft registries and offers resource conditions through Fabric API.

These differences affect mod authors and data loading, but not enumeration of recipes that successfully loaded into `RecipeManager`.

### Custom ingredients

NeoForge patches `Ingredient` with explicit custom-ingredient access:

- `ICustomIngredient`
- `IngredientType`
- `Ingredient.isCustom()` and `getCustomIngredient()`
- built-in compound, intersection, difference, component, and block-tag implementations

Fabric API injects `FabricIngredient` and provides:

- `CustomIngredient`
- `CustomIngredientSerializer`
- `FabricIngredient.getCustomIngredient()` and `requiresTesting()`
- built-in any, all, difference, component, and custom-data implementations

Both APIs warn against assuming that enumerated matching stacks are exhaustive for predicate-based ingredients. Expanding every custom ingredient into item alternatives would therefore be semantically unsafe.

Initial recipe support should either reject custom ingredients or represent only a subset whose semantics can be proven. A future loader adapter can classify custom ingredients without exposing loader classes to `core`.

### Custom recipe types

Neither loader imposes one introspection contract for custom machine recipes. Mods can register arbitrary recipe classes, serializers, inputs, outputs, fluids, energy, chances, catalysts, and contextual behavior.

Future custom-type support will require one or more of:

- project-owned adapters for known recipe types
- a capability or contributor SPI
- explicitly opaque type-specific context
- partial results marked with precise support information

A universal reflection-based normalizer is not a reliable design.

### Reload lifecycle

Recipes are datapack-reloadable, so cursor generation cannot remain constant for the process lifetime.

- Fabric API exposes `ServerLifecycleEvents.END_DATA_PACK_RELOAD`, including success state.
- NeoForge exposes reload listener and datapack synchronization events, but this reconnaissance did not establish a direct end-of-successful-server-reload event equivalent.

The NeoForge generation hook remains an implementation question. A generation must advance only after a successful recipe snapshot becomes authoritative. Requests and pagination must not cross generations silently.

### Threading

The authoritative manager is owned by `MinecraftServer`. Blockwise should retain its existing bounded server-thread dispatch for both loaders. Loader callbacks do not justify exposing manager data directly to transport threads.

## Proposed architecture boundary

The first implementation should preserve these layers:

1. A loader adapter reads `RecipeManager` on the server thread.
2. The adapter admits only supported built-in recipe types with statically representable semantics.
3. The adapter maps Minecraft values into immutable project-owned recipe models.
4. `core` validates the output item ID, applies stable ordering, and paginates against a reload generation.
5. The MCP tool maps project models into its documented schema.

The source contract should expose a coherent snapshot or generation-bearing result rather than a mutable manager. No `net.minecraft`, NeoForge, or Fabric types should enter `core`.

## Contract questions before implementation

The following require explicit decisions:

- Whether unsupported recipes are omitted, counted, or returned as structured unsupported entries.
- Whether the first milestone includes smithing transform recipes despite missing public ingredient accessors.
- How to distinguish tag ingredients from explicit item alternatives without losing source semantics.
- Whether output components are represented, summarized, or deferred while matching by item ID.
- Whether shaped layout is required or a flat ingredient list is sufficient initially.
- Which stable ordering key follows recipe ID when one recipe can expose multiple outputs.
- How NeoForge detects and publishes a successful recipe-generation change.

## Conclusion

Fabric does not require a different high-level recipe architecture. The authoritative manager and built-in recipe model are Minecraft APIs shared by both loaders. The significant portability differences are custom ingredient inspection, registration conventions, and reload lifecycle signaling. These belong behind loader adapters rather than in the shared recipe contract.
