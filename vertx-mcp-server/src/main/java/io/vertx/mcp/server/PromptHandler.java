package io.vertx.mcp.server;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.common.dsl.ArraySchemaBuilder;
import io.vertx.mcp.common.prompt.PromptMessage;

import java.util.List;
import java.util.function.Function;

/**
 * Represents a handler responsible for processing prompts. A prompt handler defines a schema for the expected arguments and provides a function to process input JSON objects and
 * produce a future result of prompt messages.
 *
 * @version 2025-06-18
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/server/prompts#prompt">Server Features - Prompts - Prompt</a>
 */
public interface PromptHandler extends Function<JsonObject, Future<List<PromptMessage>>> {

  /**
   * Creates a new instance of a {@code PromptHandler} with specified parameters.
   *
   * @param name the name of the prompt handler
   * @param title the title of the prompt handler
   * @param description the description of the prompt handler
   * @param arguments the schema builder defining the arguments expected by the prompt handler
   * @param function the function to process the input JSON and return a future list of prompt messages
   * @return a new {@code PromptHandler} instance initialized with the provided parameters
   */
  static PromptHandler create(String name, String title, String description, ArraySchemaBuilder arguments, Function<JsonObject, Future<List<PromptMessage>>> function) {
    return new PromptHandler() {
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
      public ArraySchemaBuilder arguments() {
        return arguments;
      }

      @Override
      public Future<List<PromptMessage>> apply(JsonObject args) {
        return function.apply(args);
      }
    };
  }

  /**
   * Retrieves the name of the prompt handler.
   *
   * @return the name of the prompt handler
   */
  String name();

  /**
   * Retrieves the title of the prompt handler.
   *
   * @return the title associated with this prompt handler
   */
  String title();

  /**
   * Provides the description of the prompt handler.
   *
   * @return the description of the prompt handler
   */
  String description();

  /**
   * Provides the argument schema builder for the prompt, which defines the expected structure of the arguments to be passed.
   *
   * @return the argument schema builder
   */
  ArraySchemaBuilder arguments();
}
