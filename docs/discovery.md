# Project Discovery

This document records product discovery for Blockwise MCP. It distinguishes accepted direction from provisional choices and unresolved questions. It is not an implementation specification.

## Product goal

Provide AI clients with structured, reliable access to authoritative data from a running modded Minecraft instance, avoiding stale public websites and brittle parsing of game files.

The motivating user experience is AI-assisted play, particularly understanding crafting chains and, eventually, production systems. The project is currently focused on exposing runtime data rather than implementing production optimization itself.

## Accepted decisions

- The project will use the MIT license.
- Java 21 is the project baseline for all modules running inside Minecraft.
- The base Java package and Maven group are `com.drakmyth.minecraft.blockwisemcp`; the owner controls `drakmyth.com`.
- Keep root Gradle configuration loader-neutral.
- Use NeoForge's official Minecraft 1.21.1 ModDevGradle template only as a reference for an isolated NeoForge module.
- Pin exact Gradle, ModDevGradle, and NeoForge build versions; update them explicitly.
- Require Minecraft 1.21.1 exactly and allow compatible NeoForge runtime versions in `[21.1.244,21.2)`.
- Use NeoForge's default official Mojang mappings for 1.21.1; avoid Parchment unless its added documentation becomes necessary.
- Establish a minimal NeoForge 1.21.1 mod and development environment before extracting the Minecraft-independent `core` module.
- The first functional milestone proves the mod loads in development and CI, without MCP or registry-query functionality.
- Configure both client and dedicated-server development runs to catch accidental client-only coupling early.
- CI runs NeoForge's headless GameTest server, not only compilation and packaging, to prove the mod loads.
- Log one concise startup message; do not emit startup messages to in-game chat.
- After validating the runtime, use two Gradle modules: a Minecraft-independent `core` and a `neoforge-1.21.1` module containing the adapter, embedded MCP transport, and mod entry point.
- The next milestone adds a minimal loaded-mod contract in `core`, a NeoForge implementation, and GameTest coverage without MCP transport.
- Its GameTest verifies Blockwise metadata end to end: ID, display name, and project version.
- The following milestone adds `ModService.listLoadedMods(ListLoadedModsRequest)` with filtering, stable sorting, and cursor pagination, without MCP transport.
- Keep loaded-mod domain types under `core.mods`; retain `LoadedMod` as the sole metadata model until broader reuse justifies `ModMetadata`.
- Keep loader-independent MCP transport and tool mapping in an `mcp-server` module; NeoForge composes it with runtime sources and owns endpoint lifecycle.
- Keep the initial repository template module-free; introduce modules with their first functional code rather than empty scaffolding.
- Start with EditorConfig conventions; defer an enforced Java formatter until implementation experience justifies one.
- Defer CI and Gradle setup until the first functional module provides a meaningful build and tests.
- The initial template contains `README.md`, `LICENSE`, `.gitignore`, `.editorconfig`, and `.gitattributes`.
- Keep the README concise: purpose, status, initial scope, and a discovery-document link; defer installation instructions until an artifact is runnable.
- The initial target is NeoForge for Minecraft 1.21.1.
- The provisional product name is `Blockwise MCP`, pending broader availability and branding review.
- Include `MCP` explicitly, while recognizing its ambiguity with Minecraft's former Mod Coder Pack.
- The initial system will query a running Minecraft instance.
- Offline modpack inspection is deferred, but not ruled out.
- Integrated single-player is the first-priority runtime environment.
- Multiplayer and dedicated-server support should remain on the roadmap.
- The MCP transport will use Streamable HTTP only; legacy HTTP+SSE and stdio are not initially supported.
- Use the official MCP Java SDK rather than implementing the protocol directly; pin and audit its dependencies for the embedded Minecraft environment.
- Initial releases bind strictly to `127.0.0.1`; the port is configurable and defaults to `47831`.
- Keep the Streamable HTTP path fixed at `/mcp`; coexistence with other embedded MCP mods is handled through port configuration.
- Initial endpoint configuration contains only `enabled`, `port`, and `dispatchTimeoutSeconds`.
- Investigate enforcing a fixed maximum HTTP request-body size rather than exposing it as initial configuration.
- Public remote access is deferred until authentication, authorization, transport security, resource limits, and related threats have an explicit design.
- The highest-value initial data categories are:
  1. Loaded mods
  2. Recipes
- Expose focused, single-purpose MCP tools rather than a generic data-query tool.
- Initial tools: `list_loaded_mods` and `find_recipes_by_output`.
- The next milestone delivers `list_loaded_mods` end to end through the localhost MCP endpoint before recipe implementation.
- Run the MCP endpoint only while a Minecraft server/world is active.
- Dispatch MCP tool work through the Minecraft server thread with a bounded, configurable timeout.
- MCP startup failure does not stop Minecraft; disable MCP for that server session and log a clear error without automatic retries.
- Successful tool responses use structured data without a redundant human-readable summary, subject to MCP client compatibility.
- Provisional: errors include stable machine-readable codes, concise messages, and retry guidance when applicable.
- Do not encode contract versions in tool names.
- Provisional: expose a project schema version in structured output rather than transport-specific headers.
- Before 1.0, documented schema changes may be incompatible; backward compatibility is required from 1.0 onward.
- After 1.0, minor versions may add optional fields; removals and semantic changes require a new major schema version.
- The initial loaded-mod result includes mod ID, display name, and version.
- `LoadedMod` requires non-null ID, display name, and version while preserving their loader-reported string contents exactly.
- Loaded-mod data lets AI clients scope answers and external research to mods and versions active in the current session.
- All collection-returning tools use a consistent cursor-based pagination contract, including loaded-mod listing.
- Collection queries default to 20 results and allow at most 100 results per page.
- Loaded-mod filtering uses case-insensitive substring matching over mod ID and display name; version is not searchable.
- Filtering strategies are dataset-specific. Retain the simple loaded-mod filter now, but do not default larger collections to full-scan substring matching without revisiting indexing and structured filters.
- Large datasets may require query-capable sources or indexes so filtering, ordering, and pagination occur without full materialization.
- Omitted or blank loaded-mod filters match all mods.
- Page limits outside 1 through 100 fail as invalid input rather than being silently clamped.
- Each tool defines a fixed stable ordering; client-selected sorting is not initially supported.
- Loaded-mod results sort by mod ID using natural string order.
- Runtime sources return immutable per-call lists in loader order; query and pagination services own stable sorting. Do not add ordering flags before measurement justifies them.
- Collection tools share one opaque cursor envelope containing format version, dataset generation, query identity, and position.
- Unsupported cursor format versions fail explicitly, including when a cursor spans an application upgrade.
- Cursors fail explicitly when their dataset generation is stale or their query identity does not match the request.
- Sources with static lifecycle data, such as loaded mods, may retain one generation for the process lifetime.
- Revisit generation ownership before adding reloadable datasets; services should follow one consistent generation pattern where practical.
- The first recipe operation should find recipes that produce a specified item.
- Recipe output lookup requires an exact namespaced item ID. Human-readable item search will be a separate operation.
- Initial recipe results will not include raw serialized recipes.
- First recipe milestone: given an exact item ID, return every loaded recipe producing it, including recipe ID, type, item inputs, and item outputs.
- Early development may begin with item inputs and outputs.
- A release-ready recipe contract should cover common modded semantics; exotic semantics may follow later.
- A production modpack is available for real-world contract discovery and validation.

## Provisional direction

- Prioritize fast tests for core contracts and services, supplemented by focused NeoForge integration tests for runtime extraction and lifecycle behavior.
- Provisional: pull requests to `master` must pass both core and NeoForge integration tests; avoid duplicate CI on ordinary branch pushes.
- GitHub Actions on Linux is the authoritative CI environment; local Windows development remains supported.
- Query live Minecraft managers initially; add snapshots only if measurement justifies their startup and lifecycle cost.
- Schedule bounded data extraction on Minecraft's server thread when required by API thread-safety; serialize responses off-thread.
- Fail requests explicitly when the game runtime is unavailable or bounded server-thread work times out.
- Prefer embedding the MCP server in the Minecraft process to couple its lifecycle to the game and minimize user setup.
- Keep the game-data interface sufficiently isolated that a separate MCP process could be introduced later.
- Keep stable core contracts and project-owned data models independent of NeoForge and Minecraft classes.
- Use a NeoForge 1.21.1 adapter initially; prefer loader- and version-specific adapters over a single cross-version binary.
- Treat server-owned state as authoritative, including when running an integrated server in single-player.

## Constraints and risks

- Recipes, tags, datapack content, and dynamic mod data do not all behave like Minecraft registries; the architecture should not incorrectly model every source as a registry.
- Client-only access may be incomplete or misleading in multiplayer because authoritative data can reside on the server.
- An embedded server shares Minecraft's JVM, dependency environment, failure domain, and resource limits.
- MCP requests must not access game state from unsafe threads or block the game tick for unbounded periods.
- Live-manager queries avoid snapshot startup costs but require thread-safe access and may not provide consistency across multiple requests.
- Local network transport still requires careful interface binding and endpoint lifecycle handling.
- A publicly reachable endpoint could expose mod versions, configuration, hidden content, or server internals and could enable resource-exhaustion attacks.
- Modded recipes may contain dynamic or custom ingredients and outputs that cannot be represented losslessly by one normalized universal schema.

## Open questions

- Confirm that `Blockwise MCP` is sufficiently available and compliant with Minecraft branding guidelines.
- The mod ID is `blockwisemcp`; repository and artifact names use `blockwise-mcp`.
- Which recipe operation should follow output lookup?
- How should item variants with data components be queried?
- Which semantics must the recipe contract support before its first release?
- How should an extensible, structured context represent machine settings and other type-specific recipe data?
- How should custom and dynamic recipes report information that cannot be represented faithfully?
- Should later loaded-mod results include metadata such as dependencies and source information?
- Which MCP protocol version should the initial Streamable HTTP implementation pin?
- Future: allow in-game port changes and explicit MCP start/retry controls; provide a non-UI equivalent for dedicated servers.
- What lifecycle and consistency guarantees should apply during datapack reloads?
- Which testing and documentation conventions should be adopted?
- How should MCP tools and resources expose recipe data?

## Deferred scope

- Offline inspection of installed modpacks.
- Public remote access.
- Production-line optimization as a project-owned capability.
- Runtime data categories beyond loaded mods and recipes, pending later prioritization.

## Decision history

### Initial discovery

The project began with a broad description covering recipes, items, tags, and other registry data. Discovery narrowed the first useful scope to loaded mods and recipes from a live integrated single-player runtime. Localhost-only network access was selected as the safe initial boundary, while multiplayer, dedicated servers, remote access, and offline inspection remain future considerations.
