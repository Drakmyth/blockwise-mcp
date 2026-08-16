# Cross-loader contract testing plan

## Objective

Prove that packaged Fabric and NeoForge 1.21.1 artifacts expose the same public Blockwise MCP behavior. Contract testing uses the real `/mcp` endpoint and deterministic fixtures; it does not compare loader implementation classes.

## Accepted decisions

- Use the MCP Java SDK's Streamable HTTP client against `127.0.0.1:47831/mcp`.
- Run the client inside each packaged GameTest server JVM, but communicate with Blockwise only through HTTP/MCP.
- Keep every client operation and runtime launch under explicit short time limits.
- Install deterministic test-mod recipes and metadata rather than depending on mutable vanilla datasets.
- Cover the complete current public contract. Keep success and failure scenarios in separate GameTests.
- Share protocol requests and assertions; keep fixture resources, GameTest registration, reload triggering, and runtime launch configuration loader/version-specific.

## Structure

Add a Java 21 `contract-tests` module containing no Minecraft or loader dependencies. It owns:

- bounded Streamable HTTP MCP client setup and teardown;
- initialization, tool-list, and tool-call requests;
- canonical comparisons for instructions, schemas, structured results, and error content;
- an expected-fixture manifest supplied by the loader test mod;
- focused assertion methods for separate success and failure scenarios.

Package the shared contract test classes into each GameTest artifact, not either production artifact. Reuse the MCP SDK classes already packaged with the production mod; do not embed a second SDK copy.

Each loader's 1.21.1 GameTest source set owns:

- equivalent recipe JSON fixtures and structures;
- expected loader metadata that cannot be normalized;
- thin GameTest methods invoking shared assertions;
- the loader-specific asynchronous datapack reload trigger needed for cursor invalidation.

Standardize the test mod ID as `blockwisemcp_gametest`. Loaded-mod assertions filter to Blockwise IDs so loader framework mod lists do not become contract test fixtures.

## Contract matrix

### Protocol and discovery

- Initialize successfully and compare server name, version, capabilities, and instructions.
- List tools and compare names, descriptions, input schemas, and output schemas.
- Confirm only the current public tools are advertised.

### Successful calls

- `list_loaded_mods` returns the production and GameTest mods under a narrowing Blockwise filter.
- `find_recipes_by_output` returns equivalent deterministic shaped, shapeless, cooking, and stonecutting fixtures.
- Verify structured content, text content, natural ordering, exact filtering, pagination envelopes, and cursor continuation.
- Verify omitted optional arguments and explicit supported values have equivalent behavior.

### Failure calls

- Exercise malformed, unsupported-format, stale-generation, and query-mismatch cursors separately.
- Verify stable error-code prefixes, `isError: true`, text-only content, and absent structured content.
- Exercise invalid tool arguments separately and compare protocol-level versus tool-result failures without conflating them.
- Trigger a successful loader-specific datapack reload, then prove an earlier recipe cursor is stale.

### Lifecycle

- The endpoint is available while the GameTest server is active.
- Existing bounded process completion remains the shutdown assertion. A separate-process post-shutdown probe is deferred unless lifecycle regressions show it is needed.

## Fixture rules

- Use the same recipe IDs, ingredient semantics, outputs, and counts on both loaders.
- Choose a registered output item with no vanilla recipe collisions and query by exact output ID.
- Include enough matching recipes to force pagination at page size 1.
- Include exact-item and unexpanded-tag ingredients.
- Keep fixture manifests version-specific because recipe JSON and registry behavior may change in 26.1.2.
- Treat fixture loading failures as test failures; do not silently reduce expected counts.

## Delivery sequence

Keep each PR buildable and preserve all four packaged runtime targets.

1. Add the loader-neutral contract test module, shared MCP client, initialization/tool-schema assertions, and successful loaded-mod assertions.
2. Add equivalent recipe fixtures and successful recipe filtering, ordering, schema, and pagination assertions.
3. Add cursor/error scenarios and loader-specific reload invalidation, then remove the delivered contract testing roadmap entry.

The suite is not considered delivered until all three slices run against minimum and recent packaged Fabric and NeoForge stacks.

## CI

Reuse the existing packaged compatibility jobs. Their separate GameTest artifacts gain the shared contract test classes and fixtures, so every required minimum/recent check executes the same assertions. Do not add a second source-built contract testing path that could pass while distributable artifacts fail.

Report the number of required GameTests as the existing jobs do. A future CI split may separate loader jobs for log isolation, but it is not required by this architecture.

## Outcome

The suite covers the complete initial matrix through the real MCP endpoint on minimum and recent packaged Fabric and NeoForge stacks. Shared Java 21 assertions are packaged only in test artifacts. Equivalent loader fixtures cover discovery, loaded mods, recipes, pagination, invalid arguments, all cursor error categories, and successful reload invalidation.

## Risks

- An in-JVM client does not prove cross-process connectivity, although it exercises the real socket, HTTP transport, MCP SDK, schemas, and serialization.
- MCP client dependencies may conflict with packaged server dependencies. Compile against the exact server SDK version and package only contract-test-owned classes.
- Datapack reload completion must not block the Minecraft server thread. Loader tests need asynchronous continuation with an overall timeout.
- Exact schema comparison can flag compatible SDK formatting changes. Canonicalize JSON object ordering but preserve semantic fields and array ordering.
- GameTest count and runtime will increase. Keep assertions grouped by contract scenario rather than creating one server launch per assertion.
