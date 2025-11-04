package io.vertx.mcp.server;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.prompt.PromptMessage;

import java.util.List;
import java.util.function.Function;

/**
 * Server feature for individual prompt execution.
 * Implements Function to execute the prompt with given arguments.
 * Context is obtained from Vert.x context.
 */
public interface PromptServerFeature extends ServerFeature, Function<JsonObject, Future<List<PromptMessage>>> {
}
