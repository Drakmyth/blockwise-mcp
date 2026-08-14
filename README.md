# Blockwise MCP

An embedded MCP server for querying authoritative data from a running modded Minecraft instance.

## Status

Blockwise MCP is an early, functional implementation for NeoForge on Minecraft 1.21.1. It currently exposes loaded-mod metadata from integrated single-player sessions. Public releases and recipe tools are not yet available.

## Current capabilities

- Streamable HTTP MCP endpoint at `http://127.0.0.1:47831/mcp`
- Endpoint lifecycle tied to the active Minecraft server/world
- Configurable enablement, port, and server-thread dispatch timeout
- `list_loaded_mods` with filtering, stable ordering, and cursor pagination
- Compatibility validation across NeoForge `[21.1.1,21.2)`

The endpoint is localhost-only and starts after entering a single-player world. Its production JAR is built against NeoForge 21.1.1 and tested on both the minimum and a recent NeoForge 21.1 runtime.

## Project structure

- `core`: loader-independent contracts and query services
- `mcp-server`: Streamable HTTP transport and MCP tool definitions
- `neoforge-1.21.1`: NeoForge runtime integration and packaged mod
- `neoforge-compatibility-test`: packaged-JAR compatibility harness

## Development

Requires Java 21.

```shell
./gradlew build
```

Build the production mod JAR:

```shell
./gradlew :core:build :mcp-server:build :neoforge-1.21.1:build
```

The distributable JAR is written under `neoforge-1.21.1/build/libs/`.

See [project discovery](docs/discovery.md) for accepted decisions, risks, open questions, and deferred scope.
