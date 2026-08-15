# Blockwise MCP

An embedded MCP server for querying authoritative data from a running modded Minecraft instance.

## Status

Blockwise MCP is an early, functional implementation for NeoForge and Fabric on Minecraft 1.21.1. It exposes loaded-mod metadata and statically representable recipes from integrated single-player sessions. Public releases are not yet available.

## Current capabilities

- Streamable HTTP MCP endpoint at `http://127.0.0.1:47831/mcp`
- Endpoint lifecycle tied to the active Minecraft server/world
- Configurable enablement, port, and server-thread dispatch timeout
- `list_loaded_mods` with filtering, stable ordering, and cursor pagination
- `find_recipes_by_output` for shaped, shapeless, cooking, and stonecutting recipes with static component-free outputs
- Packaged-JAR compatibility validation across NeoForge `[21.1.1,21.2)` and Fabric Loader `0.15.11` or newer with Fabric API `0.102.1+1.21.1` or newer

The endpoint is localhost-only and starts after entering a single-player world. Loader-specific production JARs are built against their minimum dependencies and tested on both minimum and recent compatible runtimes.

## Project structure

- `core`: loader-independent contracts and query services
- `mcp-server`: Streamable HTTP transport and MCP tool definitions
- `loaders/fabric/1.21.1`: Fabric runtime integration and packaged mod
- `loaders/neoforge/1.21.1`: NeoForge runtime integration and packaged mod
- `compatibility/fabric`: Fabric packaged-JAR compatibility harness
- `compatibility/neoforge`: NeoForge packaged-JAR compatibility harness

## Development

Requires Java 21.

```shell
./gradlew build
```

Build the production mod JAR:

```shell
./gradlew :core:build :mcp-server:build :loaders:fabric:1.21.1:build :loaders:neoforge:1.21.1:build
```

Loader-specific distributable JARs are written under `loaders/fabric/1.21.1/build/libs/` and `loaders/neoforge/1.21.1/build/libs/`. Published filenames follow `{modid}[-{purpose}]-{minecraft}-{version}-{loader}.jar`.

See [project discovery](docs/discovery.md) for accepted decisions and risks. Future work is prioritized in the [project roadmap](docs/roadmap.md).
