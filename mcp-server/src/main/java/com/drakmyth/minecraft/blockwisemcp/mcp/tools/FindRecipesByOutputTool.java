package com.drakmyth.minecraft.blockwisemcp.mcp.tools;

import com.drakmyth.minecraft.blockwisemcp.core.ids.ResourceId;
import com.drakmyth.minecraft.blockwisemcp.core.pagination.InvalidCursorException;
import com.drakmyth.minecraft.blockwisemcp.core.recipes.FindRecipesByOutputRequest;
import com.drakmyth.minecraft.blockwisemcp.core.recipes.RecipeDefinition;
import com.drakmyth.minecraft.blockwisemcp.core.recipes.RecipeIngredient;
import com.drakmyth.minecraft.blockwisemcp.core.recipes.RecipeInput;
import com.drakmyth.minecraft.blockwisemcp.core.recipes.RecipeOutput;
import com.drakmyth.minecraft.blockwisemcp.core.recipes.RecipeService;
import com.drakmyth.minecraft.blockwisemcp.mcp.McpToolDefinition;
import com.drakmyth.minecraft.blockwisemcp.mcp.McpToolExecutor;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FindRecipesByOutputTool {
    private static final Logger LOGGER = LoggerFactory.getLogger(FindRecipesByOutputTool.class);
    private static final Map<String, Object> INGREDIENT_SCHEMA = ingredientSchema("object");
    private static final Map<String, Object> NULLABLE_INGREDIENT_SCHEMA =
            ingredientSchema(List.of("object", "null"));

    private static final Map<String, Object> INPUT_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "outputItemId", Map.of(
                            "type", "string",
                            "pattern", "^[a-z0-9_.-]+:[a-z0-9/._-]+$",
                            "description", "Explicit namespaced item ID that every returned recipe produces"),
                    "limit", Map.of(
                            "type", "integer",
                            "minimum", 1,
                            "maximum", 100,
                            "description", "Maximum recipes to return; defaults to 20"),
                    "cursor", Map.of(
                            "type", "string",
                            "description", "Opaque continuation cursor from a previous response")),
            "required", List.of("outputItemId"),
            "additionalProperties", false);

    private static final Map<String, Object> OUTPUT_SCHEMA = outputSchema();

    private FindRecipesByOutputTool() {
    }

    public static McpToolDefinition create(RecipeService service, McpToolExecutor executor) {
        var tool = Tool.builder("find_recipes_by_output", INPUT_SCHEMA)
                .description("Finds loaded recipes that produce an exact item ID")
                .outputSchema(OUTPUT_SCHEMA)
                .build();
        var definition = SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((exchange, call) -> invoke(service, executor, Arguments.from(call.arguments())))
                .build();
        return () -> definition;
    }

    private static CallToolResult invoke(
            RecipeService service,
            McpToolExecutor executor,
            Arguments arguments) {
        var startedNanos = System.nanoTime();
        var outputItemId = arguments.outputItemId();
        var limit = arguments.limit();
        var cursor = arguments.cursor();
        try {
            var request = new FindRecipesByOutputRequest(ResourceId.parse(outputItemId), limit, cursor);
            var page = executor.execute(() -> service.findByOutput(request));
            var items = page.items().stream().map(FindRecipesByOutputTool::recipeMap).toList();
            var output = new LinkedHashMap<String, Object>();
            output.put("items", items);
            output.put("nextCursor", page.nextCursor());
            LOGGER.debug(
                    "find_recipes_by_output outputItemId={} limit={} cursorPresent={} results={} hasNext={} durationMs={}",
                    outputItemId,
                    limit == null ? "<default>" : limit,
                    cursor != null,
                    items.size(),
                    page.nextCursor() != null,
                    ToolLogging.elapsedMillis(startedNanos));
            return CallToolResult.builder().structuredContent(output).build();
        } catch (InvalidCursorException exception) {
            LOGGER.debug(
                    "find_recipes_by_output outputItemId={} limit={} cursorPresent={} failed reason={} durationMs={}",
                    outputItemId,
                    limit == null ? "<default>" : limit,
                    cursor != null,
                    exception.reason(),
                    ToolLogging.elapsedMillis(startedNanos));
            return ToolResults.failure(exception);
        } catch (Exception exception) {
            LOGGER.error(
                    "find_recipes_by_output outputItemId={} limit={} cursorPresent={} failed durationMs={}",
                    outputItemId,
                    limit == null ? "<default>" : limit,
                    cursor != null,
                    ToolLogging.elapsedMillis(startedNanos),
                    exception);
            return ToolResults.failure(exception);
        }
    }

    private record Arguments(String outputItemId, Integer limit, String cursor) {
        private static Arguments from(Map<String, Object> values) {
            var arguments = new ToolArguments(values);
            return new Arguments(
                    arguments.requiredString("outputItemId"),
                    arguments.optionalInteger("limit"),
                    arguments.optionalString("cursor"));
        }
    }

    private static Map<String, Object> recipeMap(RecipeDefinition recipe) {
        return Map.of(
                "id", recipe.id().toString(),
                "type", recipe.type().toString(),
                "input", inputMap(recipe.input()),
                "outputs", recipe.outputs().stream().map(FindRecipesByOutputTool::outputMap).toList());
    }

    private static Map<String, Object> inputMap(RecipeInput input) {
        var output = new LinkedHashMap<String, Object>();
        if (input instanceof RecipeInput.Shaped shaped) {
            output.put("format", "shaped");
            output.put("rows", shaped.rows().stream()
                    .map(row -> row.stream().map(FindRecipesByOutputTool::ingredientMap).toList())
                    .toList());
        } else if (input instanceof RecipeInput.Shapeless shapeless) {
            output.put("format", "shapeless");
            output.put("ingredients", shapeless.ingredients().stream()
                    .map(FindRecipesByOutputTool::ingredientMap)
                    .toList());
        } else if (input instanceof RecipeInput.Single single) {
            output.put("format", "single");
            output.put("ingredient", ingredientMap(single.ingredient()));
        } else {
            throw new IllegalStateException("Unknown recipe input format: " + input.getClass().getName());
        }
        return output;
    }

    private static Map<String, Object> ingredientMap(RecipeIngredient ingredient) {
        return ingredient == null
                ? null
                : Map.of("options", ingredient.options().stream().map(option -> option.selector()).toList());
    }

    private static Map<String, Object> outputMap(RecipeOutput output) {
        return Map.of("itemId", output.itemId().toString(), "count", output.count());
    }

    private static Map<String, Object> outputSchema() {
        var recipeSchema = Map.<String, Object>of(
                "type", "object",
                "properties", Map.of(
                        "id", Map.of("type", "string", "description", "Namespaced recipe ID"),
                        "type", Map.of("type", "string", "description", "Namespaced recipe serializer ID"),
                        "input", Map.of(
                                "description", "Statically representable recipe input",
                                "oneOf", List.of(shapedInputSchema(), shapelessInputSchema(), singleInputSchema())),
                        "outputs", Map.of(
                                "type", "array",
                                "description", "Static component-free outputs produced by the recipe",
                                "minItems", 1,
                                "items", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "itemId", Map.of(
                                                        "type", "string",
                                                        "description", "Namespaced output item ID"),
                                                "count", Map.of(
                                                        "type", "integer",
                                                        "minimum", 1,
                                                        "description", "Number of output items produced")),
                                        "required", List.of("itemId", "count"),
                                        "additionalProperties", false))),
                "required", List.of("id", "type", "input", "outputs"),
                "additionalProperties", false);
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "items", Map.of(
                                "type", "array",
                                "description", "Loaded recipes in this page that produce the requested item",
                                "items", recipeSchema),
                        "nextCursor", Map.of(
                                "type", List.of("string", "null"),
                                "description", "Opaque cursor for the next page, or null when no more recipes remain")),
                "required", List.of("items", "nextCursor"),
                "additionalProperties", false);
    }

    private static Map<String, Object> ingredientSchema(Object type) {
        return Map.of(
                "type", type,
                "properties", Map.of(
                        "options", Map.of(
                                "type", "array",
                                "description", "Exact item IDs or #prefixed item-tag IDs accepted by this ingredient",
                                "minItems", 1,
                                "items", Map.of("type", "string"))),
                "required", List.of("options"),
                "additionalProperties", false);
    }

    private static Map<String, Object> shapedInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "format", Map.of("const", "shaped", "description", "Shaped crafting input format"),
                        "rows", Map.of(
                                "type", "array",
                                "description", "Rectangular crafting rows with null for empty cells",
                                "minItems", 1,
                                "items", Map.of("type", "array", "minItems", 1, "items", NULLABLE_INGREDIENT_SCHEMA))),
                "required", List.of("format", "rows"),
                "additionalProperties", false);
    }

    private static Map<String, Object> shapelessInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "format", Map.of("const", "shapeless", "description", "Shapeless crafting input format"),
                        "ingredients", Map.of(
                                "type", "array",
                                "description", "Unordered ingredients consumed by the recipe",
                                "minItems", 1,
                                "items", INGREDIENT_SCHEMA)),
                "required", List.of("format", "ingredients"),
                "additionalProperties", false);
    }

    private static Map<String, Object> singleInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "format", Map.of("const", "single", "description", "Single-ingredient input format"),
                        "ingredient", Map.of(
                                "type", "object",
                                "description", "One ingredient consumed by a cooking or stonecutting recipe",
                                "properties", INGREDIENT_SCHEMA.get("properties"),
                                "required", INGREDIENT_SCHEMA.get("required"),
                                "additionalProperties", false)),
                "required", List.of("format", "ingredient"),
                "additionalProperties", false);
    }
}
