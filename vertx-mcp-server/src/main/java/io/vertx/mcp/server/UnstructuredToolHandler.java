package io.vertx.mcp.server;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.common.dsl.SchemaBuilder;
import io.vertx.mcp.common.content.Content;

import java.util.function.Function;

/**
 * Represents a handler responsible for processing unstructured tools. An unstructured tool handler defines an input schema and processes tool requests
 * with JSON input, returning unstructured content as output.
 *
 * @version 2025-06-18
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/server/tools#tool">Server Features - Tools - Tool</a>
 */
public interface UnstructuredToolHandler extends Function<JsonObject, Future<Content[]>> {

  static UnstructuredToolHandler create(SchemaBuilder inputSchema, Function<JsonObject, Future<Content[]>> function) {
    if (inputSchema == null) {
      throw new IllegalArgumentException("Input schema must not be null");
    }

    return new UnstructuredToolHandler() {
      @Override
      public SchemaBuilder inputSchema() {
        return inputSchema;
      }

      @Override
      public Future<Content[]> apply(JsonObject input) {
        return function.apply(input);
      }
    };
  }

  SchemaBuilder inputSchema();
}
