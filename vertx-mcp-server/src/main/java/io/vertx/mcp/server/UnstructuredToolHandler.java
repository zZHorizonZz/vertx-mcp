package io.vertx.mcp.server;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.mcp.common.content.Content;
import io.vertx.mcp.common.tool.Tool;

import java.util.function.Function;

/**
 * Represents a handler responsible for processing unstructured tools. An unstructured tool handler defines an input schema and processes tool requests with JSON input, returning
 * unstructured content as output.
 *
 * @version 2025-06-18
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/server/tools#tool">Server Features - Tools - Tool</a>
 */
public interface UnstructuredToolHandler extends ServerFeatureHandler<JsonObject, Future<Content[]>, Tool> {

  static UnstructuredToolHandler create(String name, ObjectSchemaBuilder inputSchema, Function<JsonObject, Future<Content[]>> function) {
    return create(name, null, null, inputSchema, function);
  }

  static UnstructuredToolHandler create(String name, String title, ObjectSchemaBuilder inputSchema, Function<JsonObject, Future<Content[]>> function) {
    return create(name, title, null, inputSchema, function);
  }

  static UnstructuredToolHandler create(String name, String title, String description, ObjectSchemaBuilder inputSchema, Function<JsonObject, Future<Content[]>> function) {
    if (inputSchema == null) {
      throw new IllegalArgumentException("Input schema must not be null");
    }

    return new UnstructuredToolHandler() {
      @Override
      public String name() {
        return name;
      }

      @Override
      public String title() {
        return title;
      }

      @Override
      public String description() {
        return description;
      }

      @Override
      public ObjectSchemaBuilder inputSchema() {
        return inputSchema;
      }

      @Override
      public Future<Content[]> apply(JsonObject input) {
        return function.apply(input);
      }
    };
  }

  @Override
  default Tool toFeature() {
    Tool tool = new Tool()
      .setName(name())
      .setInputSchema(inputSchema().toJson());

    if (title() != null) {
      tool.setTitle(title());
    }
    if (description() != null) {
      tool.setDescription(description());
    }

    return tool;
  }

  ObjectSchemaBuilder inputSchema();
}
