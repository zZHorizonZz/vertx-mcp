package io.vertx.mcp.server;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.mcp.common.tool.Tool;

import java.util.function.Function;

/**
 * Represents a handler responsible for processing structured tools. A structured tool handler defines both input and output schemas and processes tool requests with structured
 * JSON input and output.
 *
 * @version 2025-06-18
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/server/tools#tool">Server Features - Tools - Tool</a>
 */
public interface StructuredToolHandler extends ServerFeatureHandler<JsonObject, Future<JsonObject>, Tool> {

  static StructuredToolHandler create(String name, ObjectSchemaBuilder inputSchema, ObjectSchemaBuilder outputSchema, Function<JsonObject, Future<JsonObject>> function) {
    return create(name, null, null, inputSchema, outputSchema, function);
  }

  static StructuredToolHandler create(String name, String title, ObjectSchemaBuilder inputSchema, ObjectSchemaBuilder outputSchema,
    Function<JsonObject, Future<JsonObject>> function) {
    return create(name, title, null, inputSchema, outputSchema, function);
  }

  static StructuredToolHandler create(String name, String title, String description, ObjectSchemaBuilder inputSchema, ObjectSchemaBuilder outputSchema,
    Function<JsonObject, Future<JsonObject>> function) {
    if (inputSchema == null) {
      throw new IllegalArgumentException("Input schema must not be null");
    }
    if (outputSchema == null) {
      throw new IllegalArgumentException("Output schema must not be null");
    }

    return new StructuredToolHandler() {
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
      public ObjectSchemaBuilder outputSchema() {
        return outputSchema;
      }

      @Override
      public Future<JsonObject> apply(JsonObject input) {
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
    if (outputSchema() != null) {
      tool.setOutputSchema(outputSchema().toJson());
    }

    return tool;
  }

  ObjectSchemaBuilder inputSchema();

  ObjectSchemaBuilder outputSchema();
}
