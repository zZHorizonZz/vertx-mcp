package io.vertx.mcp.common;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonSchema;
import io.vertx.mcp.common.content.Content;

import java.util.function.Function;

public interface ModelContextProtocolTool extends Function<JsonObject, Future<Content>> {
  String id();

  String name();

  String title();

  String description();

  JsonSchema inputSchema();

  JsonSchema outputSchema();
}
