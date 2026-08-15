# Toolchain and consistency audit

## Accepted target

- Preserve minimum and recent packaged compatibility checks for both loaders in every intermediate PR.
- Prefix equivalent loader-owned classes with `Fabric` or `NeoForge`, even inside loader packages.
- Organize production adapters under `loaders/<loader>/<minecraft-version>` and packaged compatibility harnesses under `compatibility/<loader>`.
- Keep `core` and `mcp-server` loader-neutral.

Target layout:

```text
core/
mcp-server/
loaders/
  fabric/1.21.1/
  neoforge/1.21.1/
compatibility/
  fabric/
  neoforge/
```

## Findings

### Toolchain

- Current versions are Gradle 9.2.1, Fabric Loom 1.15.5, and ModDevGradle 2.0.143.
- Latest stable mutually compatible candidates are Gradle 9.7.0, Fabric Loom 1.17.19, and ModDevGradle 2.0.144. Loom 1.17.19 requires Gradle 9.5 or newer.
- ModDevGradle's version is duplicated in two build scripts while Loom is property-driven.
- Runtime baselines are independent from build plugins and must remain pinned to the existing minimum/recent matrices.

### Naming and packages

- Entrypoints are named `BlockwiseFabric` and `Blockwise`; neither follows the accepted loader-prefix convention.
- Both loaders define an unqualified `MinecraftServerToolExecutor`.
- NeoForge configuration is `BlockwiseConfig`, while Fabric configuration is `FabricConfig`.
- GameTest classes are `BlockwiseFabricGameTests` and `BlockwiseGameTests`.
- NeoForge's entrypoint and GameTests sit outside its loader package; Fabric loader-owned code is consistently under its loader package.

Target names are `FabricBlockwiseMcp`, `NeoForgeBlockwiseMcp`, `FabricMinecraftServerToolExecutor`, `NeoForgeMinecraftServerToolExecutor`, `FabricConfig`, `NeoForgeConfig`, `FabricBlockwiseGameTests`, and `NeoForgeBlockwiseGameTests`.

### Implementation patterns

- Loader lifecycle composition, tool ordering, generation creation, endpoint logging, and server-thread execution are behaviorally aligned.
- The two server-thread executors are byte-for-byte equivalent apart from package names. Keep explicit loader adapters for now rather than introducing a Minecraft-version common module before another loader establishes a stable boundary.
- Lifecycle visibility differs because NeoForge event registration requires public methods while Fabric callbacks do not. This is loader-driven, not drift.
- Fabric isolates GameTests in a separate mod; NeoForge ships GameTests and structures in the production artifact. This is a real packaging inconsistency and should be investigated before deciding whether NeoForge can adopt a separate test mod without weakening exact-artifact validation.
- Fabric configuration has focused unit tests; NeoForge configuration does not. Equivalent validation behavior should receive equivalent test coverage where its API permits isolated tests.
- Compatibility harnesses intentionally differ: Fabric launches Loader's production server directly, while NeoForge uses ModDevGradle with the packaged JAR as an external runtime mod. Their contracts and naming can align even when implementation cannot.

## Risks

- Combining directory moves, public class renames, test packaging changes, and tool upgrades would obscure failures and make rollback difficult.
- Directory moves affect Gradle paths, CI filters, artifact paths, README commands, and ignore rules.
- A separate NeoForge GameTest mod may require loader-specific metadata or ModDevGradle capabilities not yet proven.
- Toolchain upgrades may alter remapping, nested dependency packaging, configuration-cache behavior, or GameTest task wiring.

## Proposed delivery

1. Move modules to the accepted directory layout and update paths only.
2. Rename loader-owned classes and normalize packages without behavioral changes.
3. Upgrade Gradle to 9.7.0, Loom to 1.17.19, and ModDevGradle to 2.0.144; centralize plugin versions.
4. Investigate and, if viable, isolate NeoForge GameTests from the production artifact and close configuration-test gaps.
5. Build the cross-loader conformance suite on the normalized structure.

Each implementation PR must pass unit tests, loader development tests, production artifact inspection, and all four minimum/recent packaged compatibility checks.
