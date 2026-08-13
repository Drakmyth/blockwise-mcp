# Project Discovery

This document records product discovery for Minecraft Registry MCP. It distinguishes accepted direction from provisional choices and unresolved questions. It is not an implementation specification.

## Product goal

Provide AI clients with structured, reliable access to authoritative data from a running modded Minecraft instance, avoiding stale public websites and brittle parsing of game files.

The motivating user experience is AI-assisted play, particularly understanding crafting chains and, eventually, production systems. The project is currently focused on exposing runtime data rather than implementing production optimization itself.

## Accepted decisions

- The project will use the MIT license.
- The implementation language is Java 21.
- The base Java package and Maven group are `com.drakmyth.minecraft.registrymcp`; the owner controls `drakmyth.com`.
- Establish a minimal NeoForge 1.21.1 mod and development environment before extracting the Minecraft-independent `core` module.
- The first functional milestone proves the mod loads in development and CI, without MCP or registry-query functionality.
- Configure both client and dedicated-server development runs to catch accidental client-only coupling early.
- CI runs NeoForge's headless GameTest server, not only compilation and packaging, to prove the mod loads.
- After validating the runtime, use two Gradle modules: a Minecraft-independent `core` and a `neoforge-1.21.1` module containing the adapter, embedded MCP transport, and mod entry point.
- Extract a separate MCP server module only when another deployment requires it.
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
- The MCP transport will be network-based rather than stdio-oriented.
- Initial releases will bind to localhost by default.
- Public remote access is deferred until authentication, authorization, transport security, resource limits, and related threats have an explicit design.
- The highest-value initial data categories are:
  1. Loaded mods
  2. Recipes
- Expose focused, single-purpose MCP tools rather than a generic data-query tool.
- Initial tools: `list_loaded_mods` and `find_recipes_by_output`.
- Successful tool responses use structured data without a redundant human-readable summary, subject to MCP client compatibility.
- Provisional: errors include stable machine-readable codes, concise messages, and retry guidance when applicable.
- Do not encode contract versions in tool names.
- Provisional: expose a project schema version in structured output rather than transport-specific headers.
- Before 1.0, documented schema changes may be incompatible; backward compatibility is required from 1.0 onward.
- After 1.0, minor versions may add optional fields; removals and semantic changes require a new major schema version.
- The initial loaded-mod result includes mod ID, display name, and version.
- Loaded-mod data lets AI clients scope answers and external research to mods and versions active in the current session.
- All collection-returning tools use a consistent cursor-based pagination contract, including loaded-mod listing.
- Each tool defines a fixed stable ordering; client-selected sorting is not initially supported.
- Provisional: cursors carry a dataset generation and fail explicitly when a reload makes them stale.
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
- The mod ID is `blockwise`.
- Which recipe operation should follow output lookup?
- How should item variants with data components be queried?
- Which semantics must the recipe contract support before its first release?
- How should an extensible, structured context represent machine settings and other type-specific recipe data?
- How should custom and dynamic recipes report information that cannot be represented faithfully?
- Should later loaded-mod results include metadata such as dependencies and source information?
- Which MCP network transport and protocol version should be supported?
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
