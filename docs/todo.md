# Project TODO

## Release hardening

- Validate the production JAR in the Infinity Legacy II world on Minecraft 1.21.1 and NeoForge 21.1.235.
- Confirm both MCP tools are discoverable and representative pack mods appear in `list_loaded_mods`.
- Verify vanilla shaped, shapeless, cooking, and stonecutting recipe mappings.
- Verify shaped empty cells, exact item alternatives, and unexpanded `#`-prefixed tags.
- Verify mod-namespaced outputs and recipes with multiple alternatives.
- Verify recipe pagination and empty results for an unknown valid item ID.
- Confirm smithing, dynamic, custom-ingredient, and component-bearing recipes are not misrepresented.
- Capture a recipe cursor, run `/reload`, and confirm the old cursor is rejected.
- Record missing or inaccurate recipes with both recipe and output IDs.

## Recipe contract follow-up

- Add experience yield to applicable recipe contracts.
- Add processing duration in ticks to applicable recipe contracts.
- Decide whether non-applicable recipe metadata is omitted, nullable, or represented through type-specific context before changing the MCP schema.
