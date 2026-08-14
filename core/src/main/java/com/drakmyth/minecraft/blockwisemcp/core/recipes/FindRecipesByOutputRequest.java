package com.drakmyth.minecraft.blockwisemcp.core.recipes;

/**
 * Requests a page of recipes producing one exact item ID.
 *
 * @param outputItemId required namespaced output item ID
 * @param limit optional page size; defaults to 20 and must be between 1 and 100
 * @param cursor optional opaque continuation cursor
 */
public record FindRecipesByOutputRequest(String outputItemId, Integer limit, String cursor) {
}
