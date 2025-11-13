package io.vertx.mcp.server;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.common.dsl.SchemaBuilder;

import java.util.function.Function;

public interface StructuredToolHandler extends Function<JsonObject, Future<JsonObject>> {

  static StructuredToolHandler create(SchemaBuilder inputSchema, SchemaBuilder outputSchema, Function<JsonObject, Future<JsonObject>> function) {
    if (inputSchema == null) {
      throw new IllegalArgumentException("Input schema must not be null");
    }

    if (outputSchema == null) {
      throw new IllegalArgumentException("Output schema must not be null");
    }

    return new StructuredToolHandler() {
      @Override
      public SchemaBuilder inputSchema() {
        return inputSchema;
      }

      @Override
      public SchemaBuilder outputSchema() {
        return outputSchema;
      }

      @Override
      public Future<JsonObject> apply(JsonObject input) {
        return function.apply(input);
      }
    };
  }

  SchemaBuilder inputSchema();

  SchemaBuilder outputSchema();
}
