# NeoForge test isolation reconnaissance

## Goal

Match Fabric's production-artifact hygiene by testing the exact NeoForge production JAR alongside a separate GameTest mod, without weakening minimum/recent runtime validation.

## Findings

- ModDevGradle supports isolated source sets through `neoForge.addModdingDependenciesTo`.
- Multiple source sets can be registered as distinct development mods under `neoForge.mods`.
- A dedicated `Jar` task can package an isolated source set independently.
- On Minecraft 1.21.1, an external runtime JAR must contain `META-INF/neoforge.mods.toml` or declare an appropriate `FMLModType` manifest entry.
- The GameTest mod can compile against the production source-set output in development while depending on the separately loaded production JAR in packaged validation.
- GameTest structures belong with the GameTest mod rather than the production artifact.
- The compatibility harness must accept both exact artifacts and load them together under the selected NeoForge runtime.
- ModDevGradle also supports JUnit tests with a configured tested mod. This provides a supported path for focused `NeoForgeConfig` default and range coverage.

## Proposed structure

```text
loaders/neoforge/1.21.1/
  src/main/
  src/gametest/
    java/
    resources/META-INF/neoforge.mods.toml
```

The production mod remains `blockwisemcp`; the test-only mod uses `blockwisemcp_gametest`. GameTest classes and structures move to `src/gametest`. Development runs load both source sets, while packaged validation loads the production and GameTest JARs as external mods.

## Artifact naming

Published archives use Gradle's identity-first components:

```text
{modid}[-{purpose}]-{minecraft}-{version}-{loader}.jar
```

Examples:

```text
blockwisemcp-1.21.1-0.1.0-SNAPSHOT-neoforge.jar
blockwisemcp-gametest-1.21.1-0.1.0-SNAPSHOT-neoforge.jar
blockwisemcp-1.21.1-0.1.0-SNAPSHOT-fabric.jar
blockwisemcp-gametest-1.21.1-0.1.0-SNAPSHOT-fabric.jar
```

Auxiliary published artifacts place their purpose after the mod ID. Internal compatibility harness outputs are not published and do not need this convention.

## Risks and validation

- NeoForge GameTest discovery must still find tests held by the test-only mod while their namespace remains `blockwisemcp`.
- The test mod must express a required dependency on the exact production mod ID without coupling to the project snapshot version.
- The production JAR must be inspected to prove it contains no GameTest classes, structures, or test metadata.
- Both NeoForge runtime targets must pass nonzero packaged tests with the two external artifacts.
- Artifact selectors in CI must avoid ambiguous globbing once production and GameTest JARs share a directory.

## Delivery recommendation

Use one focused PR for cross-loader artifact naming, since CI consumes both loaders' filenames. Follow with a separate NeoForge GameTest-isolation and configuration-test PR. This keeps naming failures distinct from runtime test-discovery failures.
