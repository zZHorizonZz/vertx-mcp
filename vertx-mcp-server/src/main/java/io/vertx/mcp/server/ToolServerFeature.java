package io.vertx.mcp.server;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.util.function.Function;

/**
 * Server feature for individual tool execution.
 * Implements Function to execute the tool with given arguments.
 * Context is obtained from Vert.x context.
 */
public interface ToolServerFeature extends ServerFeature, Function<JsonObject, Future<JsonObject>> {
}
