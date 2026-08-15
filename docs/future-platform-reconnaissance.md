# Future platform reconnaissance

Checked 2026-08-15. This is planning evidence, not a support claim.

## Accepted scope

- Forge and Quilt are intended future production targets.
- Minecraft 26.1.2 is the preferred next Minecraft version after 1.21.1.
- Preview or beta dependencies may inform planning, but implementation requires a production-ready loader and toolchain stack.
- Conformance must precede platform expansion and test observable behavior rather than shared adapter internals.

## Availability findings

| Platform | 1.21.1 | 26.1.2 | Planning conclusion |
| --- | --- | --- | --- |
| Fabric | Established production stack | Stable Minecraft release and multiple non-preview Fabric API releases; Loader 0.19.3 is game-version independent | Viable next-version candidate, subject to a packaged prototype and stable Loom verification |
| NeoForge | Established production stack | NeoForge has progressed from beta builds to non-beta 26.1.2 releases | Viable next-version candidate, subject to packaged minimum/recent stack selection |
| Forge | Production releases exist | Forge progressed from 64.0.x to 64.1.x releases for 26.1.2 | Viable future loader; requires a distinct ForgeGradle adapter and compatibility harness |
| Quilt | Quilt Loader recognizes both game versions as stable | Quilt Loader is available, but Quilt Standard Libraries and Quilted Fabric API have no 26.1.2 release | Treat Fabric-artifact compatibility as a hypothesis; do not promise Quilt support until exact packaged validation passes |

Evidence sources:

- [Mojang version manifest](https://piston-meta.mojang.com/mc/game/version_manifest_v2.json)
- [Fabric API Maven metadata](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml)
- [Fabric Loader Maven metadata](https://maven.fabricmc.net/net/fabricmc/fabric-loader/maven-metadata.xml)
- [NeoForge Maven metadata](https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml)
- [Forge Maven metadata](https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml)
- [Quilt Loader Maven metadata](https://maven.quiltmc.org/repository/release/org/quiltmc/quilt-loader/maven-metadata.xml)
- [Quilt game-version metadata](https://meta.quiltmc.org/v3/versions/game)
- [Quilt Standard Libraries metadata](https://maven.quiltmc.org/repository/release/org/quiltmc/qsl/maven-metadata.xml)
- [Quilted Fabric API metadata](https://maven.quiltmc.org/repository/release/org/quiltmc/quilted-fabric-api/quilted-fabric-api/maven-metadata.xml)

## Version boundary

Minecraft 26.1.2 is a larger boundary than another 1.21.x module:

- It requires Java 25 (`java-runtime-epsilon`), while 1.21.1 uses Java 21.
- Mojang's 26.1.2 manifest distributes no client or server mapping downloads. Build pipelines must not assume the old obfuscation/mapping model.
- Loader, Minecraft API, data-pack, GameTest, and recipe-mapping changes must remain inside version-specific production and compatibility modules.
- Root, `core`, and `mcp-server` should retain their oldest required Java target until a concrete shared-code requirement proves otherwise.

The existing `loaders/<loader>/<minecraft-version>` layout remains appropriate. Compatibility modules need an explicit Minecraft-version dimension before adding 26.1.2, for example `compatibility/<loader>/<minecraft-version>`.

## Loader boundary

- Fabric and Quilt may share runtime behavior, but support must be defined by packaged validation rather than assumed compatibility. A separate Quilt adapter is unnecessary if the unchanged Fabric artifact passes the complete Quilt matrix.
- Forge and NeoForge should remain explicit adapters. Similar APIs do not justify a shared production abstraction before both implementations expose a stable boundary.
- Each supported loader/version combination owns lifecycle wiring, config integration, ingredient introspection, reload signaling, test metadata, and runtime launch configuration.
- Public artifact names continue to identify the loader they were built and validated for. Reusing a Fabric artifact on Quilt requires an explicit publication/support decision.

## Conformance implications

Build conformance before onboarding another platform. Its shared layer should assert only MCP-observable contracts:

- initialization instructions and tool schemas;
- loaded-mod and recipe fixture results;
- ordering, filtering, pagination, and cursor failures;
- stable runtime error codes;
- reload generation invalidation;
- lifecycle availability while a server is active.

Keep launchers, GameTest registration, fixture resources, expected loader metadata, and runtime matrices loader/version-specific. The suite should accept a launched endpoint and expected fixture manifest rather than importing loader classes. This permits the same assertions to run against Java 21 and Java 25 processes.

## Risks

- Calling non-preview coordinates "production-ready" is insufficient by itself; exact packaged startup, GameTests, artifact inspection, and minimum/recent runtime selection remain mandatory.
- Quilt's current API gap may make a native Quilt adapter impractical even though Quilt Loader supports the game version.
- A shared Fabric/Quilt artifact could make loader-specific publication naming misleading.
- Moving shared modules to Java 25 would unnecessarily drop 1.21.1 compatibility.
- A Cartesian CI matrix can grow quickly. Add one loader or Minecraft-version axis per focused PR.

## Recommended sequence

1. Implement external, observable conformance for Fabric 1.21.1 and NeoForge 1.21.1.
2. Prototype 26.1.2 build and packaged startup separately for Fabric and NeoForge; select production minimum/recent coordinates only after results are reviewed.
3. Add 26.1.2 production modules while retaining Java 21 shared outputs.
4. Validate the existing Fabric artifact on Quilt before deciding whether Quilt needs its own adapter or artifact.
5. Add Forge through an explicit adapter and ForgeGradle compatibility harness.
6. Generalize only boundaries demonstrated by these implementations.
