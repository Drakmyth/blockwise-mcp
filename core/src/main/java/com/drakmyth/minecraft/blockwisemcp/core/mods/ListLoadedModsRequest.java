package com.drakmyth.minecraft.blockwisemcp.core.mods;

/**
 * Requests a page of loaded mods.
 *
 * @param filter optional case-insensitive ID or display-name substring; blank matches all
 * @param limit optional page size; defaults to 20 and must be between 1 and 100
 * @param cursor optional opaque continuation cursor
 */
public record ListLoadedModsRequest(String filter, Integer limit, String cursor) {
}
