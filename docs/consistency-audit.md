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

- The build uses Gradle 9.7.0, Fabric Loom 1.17.19, and ModDevGradle 2.0.144.
- Loom and ModDevGradle versions are centralized in Gradle properties.
- Runtime baselines remain independently pinned to the existing minimum/recent matrices.

### Naming and packages

- Loader-owned classes use the accepted `Fabric` or `NeoForge` prefix consistently.
- Entrypoints, executors, configuration, lifecycle adapters, runtime sources, and GameTests are all distinguished without relying on package context.
- All loader-owned classes reside beneath their loader package.

### Implementation patterns

- Loader lifecycle composition, tool ordering, generation creation, endpoint logging, and server-thread execution are behaviorally aligned.
- The two server-thread executors are byte-for-byte equivalent apart from package names. Keep explicit loader adapters for now rather than introducing a Minecraft-version common module before another loader establishes a stable boundary.
- Lifecycle visibility differs because NeoForge event registration requires public methods while Fabric callbacks do not. This is loader-driven, not drift.
- Both loaders isolate GameTests and test resources in separate test-only mods while packaged validation exercises the exact production JAR beside them.
- Both loader configurations have focused unit coverage for defaults and accepted ranges.
- Compatibility harnesses intentionally differ: Fabric launches Loader's production server directly, while NeoForge uses ModDevGradle with the packaged JAR as an external runtime mod. Their contracts and naming can align even when implementation cannot.

## Risks

- Combining directory moves, public class renames, test packaging changes, and tool upgrades would obscure failures and make rollback difficult.
- Directory moves affect Gradle paths, CI filters, artifact paths, README commands, and ignore rules.
- A separate NeoForge GameTest mod may require loader-specific metadata or ModDevGradle capabilities not yet proven.
- Toolchain upgrades may alter remapping, nested dependency packaging, configuration-cache behavior, or GameTest task wiring.

## Proposed delivery

1. Build the cross-loader conformance suite on the normalized structure.

Each implementation PR must pass unit tests, loader development tests, production artifact inspection, and all four minimum/recent packaged compatibility checks.
