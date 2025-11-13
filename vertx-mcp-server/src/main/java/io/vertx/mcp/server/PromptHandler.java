package io.vertx.mcp.server;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.common.dsl.ArraySchemaBuilder;
import io.vertx.json.schema.common.dsl.SchemaBuilder;
import io.vertx.mcp.common.prompt.PromptMessage;

import java.util.List;
import java.util.function.Function;

/**
 * Handler for MCP prompts. Prompts can have optional input schema for arguments.
 */
public interface PromptHandler extends Function<JsonObject, Future<List<PromptMessage>>> {

  /**
   * Creates a new prompt handler.
   *
   * @param argumentsSchema Schema builder for prompt arguments (can be null)
   * @param function function that generates the prompt messages
   * @return a new prompt handler
   */
  static PromptHandler create(ArraySchemaBuilder argumentsSchema, Function<JsonObject, Future<List<PromptMessage>>> function) {
    return new PromptHandler() {
      @Override
      public ArraySchemaBuilder arguments() {
        return argumentsSchema;
      }

      @Override
      public Future<List<PromptMessage>> apply(JsonObject args) {
        return function.apply(args);
      }
    };
  }

  /**
   * Gets the arguments schema for this prompt.
   *
   * @return SchemaBuilder, or null if no arguments
   */
  ArraySchemaBuilder arguments();
}
