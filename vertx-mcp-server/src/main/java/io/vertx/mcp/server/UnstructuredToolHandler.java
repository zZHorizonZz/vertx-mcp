package io.vertx.mcp.server;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.content.Content;

import java.util.function.Function;

public interface UnstructuredToolHandler extends Function<JsonObject, Future<Content[]>> {

  static UnstructuredToolHandler create(JsonObject inputSchema, Function<JsonObject, Future<Content[]>> function) {
    if (inputSchema == null) {
      throw new IllegalArgumentException("Input schema must not be null");
    }

    return new UnstructuredToolHandler() {
      @Override
      public JsonObject inputSchema() {
        return inputSchema;
      }

      @Override
      public Future<Content[]> apply(JsonObject input) {
        return function.apply(input);
      }
    };
  }

  JsonObject inputSchema();
}
