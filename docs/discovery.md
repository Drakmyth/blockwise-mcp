# Project Discovery

This document records decisions and unresolved product questions for Blockwise MCP. Implementation details belong in code and operating guidance belongs in `AGENTS.md`.

## Product goal

Provide AI clients with structured access to authoritative data from a running modded Minecraft instance. The motivating use case is AI-assisted play, beginning with loaded-mod awareness and recipe discovery.

## Accepted decisions

### Product and runtime

- Query the running Minecraft instance; offline modpack inspection is deferred.
- Target integrated single-player first. Keep multiplayer and dedicated servers on the roadmap.
- Support Minecraft 1.21.1 with NeoForge runtime versions in `[21.1.1,21.2)`.
- Use Java 21, Gradle, official Mojang mappings, and the MIT license.
- Use `Blockwise MCP` as the project name, `blockwisemcp` as the mod ID, and `com.drakmyth.minecraft.blockwisemcp` as the base package.
- Prioritize loaded mods, then recipe lookup by exact output item ID.

### Architecture and lifecycle

- Keep the root build loader-neutral.
- `core` owns Minecraft-independent contracts and services.
- `mcp-server` owns loader-independent MCP transport and tool mapping.
- Loader modules compose runtime sources, threading, configuration, and endpoint lifecycle.
- Use the official MCP Java SDK and Streamable HTTP only.
- Embed the MCP server in the Minecraft process.
- Run the endpoint only while a Minecraft server/world is active.
- Dispatch authoritative runtime work through the Minecraft server thread with a bounded timeout.
- On endpoint startup failure, keep Minecraft running, disable MCP for that server session, and log the failure without retrying automatically.
- Bind strictly to `127.0.0.1`, use the fixed `/mcp` path, default to port `47831`, and limit requests to 1 MiB.
- Initial configuration contains `enabled`, `port`, and `dispatchTimeoutSeconds`.
- Preserve SDK JSON Schema validation under NeoForge by repackaging networknt schema resources beneath a module-visible package and redirecting scoped resource lookup.
- Keep `McpToolDefinition` as the project-owned SDK registration boundary until more tools justify a richer abstraction.

### Tool contracts

- Expose focused tools rather than a generic query language.
- The planned initial tools are `list_loaded_mods` and `find_recipes_by_output`.
- Successful tool responses use structured content without a redundant prose summary.
- Do not encode contract versions in tool names.
- Before 1.0, documented schemas may change incompatibly. From 1.0 onward, removals and semantic changes require a major schema version.
- Describe every published input and output property in its JSON Schema.
- Paginated tools use the shared `items` and `nextCursor` fields; descriptions provide domain meaning without changing the envelope.
- Collection tools default to 20 results and allow at most 100 per page.
- Each collection tool defines stable ordering; client-selected sorting is not initially supported.
- Collection cursors are opaque and encode format version, dataset generation, query identity, and position.
- Reject unsupported, stale, or request-mismatched cursors explicitly.
- Static lifecycle data may use one generation for the process lifetime; revisit generation ownership for reloadable data.

### Loaded mods

- `list_loaded_mods` returns loader-reported mod ID, display name, and version.
- Preserve loader-reported strings exactly and require them to be non-null.
- Filter by case-insensitive substring over mod ID and display name; version is not searchable.
- Omitted or blank filters match all mods.
- Sort by mod ID using natural string order.
- Reject limits outside 1 through 100 rather than clamping them.
- Runtime sources return immutable per-call lists in loader order; services own filtering, sorting, and pagination.

### Recipe direction

- The first recipe operation finds every loaded recipe producing an exact namespaced item ID.
- Initial support covers Minecraft's built-in recipe types, including recipes supplied by mod namespaces.
- Admit a recipe only when its inputs and output are statically and authoritatively representable; defer custom machine types and input-dependent outputs.
- Initial results include recipe ID, type, item inputs, and item outputs.
- Do not expose raw serialized recipes initially.
- Keep loader-specific custom ingredient, registration, and reload mechanisms behind adapters; detailed findings are in [recipe portability reconnaissance](recipe-portability.md).
- The release-ready contract must represent common modded semantics; exotic or dynamic semantics may follow.

### Build and validation

- Pin Gradle, ModDevGradle, and NeoForge versions explicitly.
- Build the production JAR against NeoForge 21.1.1.
- Run that exact JAR as an external mod under GameTests on both 21.1.1 and a pinned recent 21.1.x release.
- Update the recent NeoForge pin periodically rather than resolving it dynamically.
- Require stable `build`, `validation (minimum)`, and `validation (latest)` CI checks.
- Skip expensive CI work when every changed file is Markdown or under `docs/`.
- Allow trusted same-repository pull requests to write Gradle caches; keep fork pull requests read-only.

## Provisional direction

- Query live Minecraft managers first; add snapshots or indexes only when dataset size or measurements justify them.
- Return stable machine-readable error codes with concise messages and retry guidance where applicable.
- Expose a project schema version in structured output if contract evolution requires it.
- Keep adapters isolated enough to support additional loaders and Minecraft versions without coupling them into `core`.

## Risks

- Recipes, tags, datapacks, and dynamic mod data do not share one universal registry model.
- Client-side data may be incomplete or non-authoritative in multiplayer.
- Embedded transport shares Minecraft's JVM, dependency environment, resource limits, and failure domain.
- Runtime requests must not access game state from unsafe threads or block ticks without a bound.
- Public network access would require authentication, authorization, transport security, and resource controls.
- Modded recipes may contain custom or dynamic semantics that cannot be normalized losslessly.
- SecureJarHandler does not find non-package resources in readable parent-layer modules through fallback lookup; an upstream report requires a minimal reproduction.

## Open questions

- Is `Blockwise MCP` sufficiently available and compliant with Minecraft branding guidance?
- Which recipe operation should follow output lookup?
- How should item variants with data components be queried?
- Should unsupported recipes be omitted, counted, or returned as structured unsupported entries?
- Should initial support include smithing transforms despite missing public generic ingredient accessors?
- How should tags, item alternatives, shaped layouts, and output components be represented without losing semantics?
- How should NeoForge detect and publish a successful recipe-generation change after datapack reload?
- How should type-specific recipe context, custom recipes, and dynamic recipes report semantics that cannot be represented faithfully?
- Should loaded-mod results later include dependencies or source information?
- Which MCP protocol version should the first release pin?
- What lifecycle and consistency guarantees should apply during datapack reloads?

## Deferred scope

- Offline modpack inspection.
- Public or remote access.
- Multiplayer and dedicated-server support.
- In-game endpoint controls and automatic startup retry.
- Production-line optimization as a project-owned capability.
- Runtime data categories beyond loaded mods and recipes.
