package io.vertx.mcp.server;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.common.dsl.ArraySchemaBuilder;
import io.vertx.mcp.common.completion.Completion;
import io.vertx.mcp.common.completion.CompletionArgument;
import io.vertx.mcp.common.completion.CompletionContext;
import io.vertx.mcp.common.prompt.Prompt;
import io.vertx.mcp.common.prompt.PromptArgument;
import io.vertx.mcp.common.prompt.PromptMessage;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Represents a handler responsible for processing prompts. A prompt handler defines a schema for the expected arguments and provides a function to process input JSON objects and
 * produce a future result of prompt messages.
 *
 * @version 2025-06-18
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/server/prompts#prompt">Server Features - Prompts - Prompt</a>
 */
@VertxGen
public interface PromptHandler extends ServerFeatureHandler<JsonObject, Future<List<PromptMessage>>, Prompt> {

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
  @GenIgnore
  static PromptHandler create(String name, String title, String description, ArraySchemaBuilder arguments, Function<JsonObject, Future<List<PromptMessage>>> function) {
    return create(name, title, description, arguments, function, null);
  }

  /**
   * Creates a new instance of a {@code PromptHandler} with specified parameters including a completion function.
   *
   * @param name the name of the prompt handler
   * @param title the title of the prompt handler
   * @param description the description of the prompt handler
   * @param arguments the schema builder defining the arguments expected by the prompt handler
   * @param function the function to process the input JSON and return a future list of prompt messages
   * @param completionFunction the function to provide completion suggestions for arguments
   * @return a new {@code PromptHandler} instance initialized with the provided parameters
   */
  @GenIgnore
  static PromptHandler create(String name, String title, String description, ArraySchemaBuilder arguments,
    Function<JsonObject, Future<List<PromptMessage>>> function,
    BiFunction<CompletionArgument, CompletionContext, Future<Completion>> completionFunction) {
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

      @Override
      public Future<Completion> completion(CompletionArgument argument, CompletionContext context) {
        if (completionFunction != null) {
          return completionFunction.apply(argument, context);
        }
        return null;
      }
    };
  }

  @Override
  default Prompt toFeature() {
    Prompt prompt = new Prompt().setName(name());

    if (title() != null) {
      prompt.setTitle(title());
    }
    if (description() != null) {
      prompt.setDescription(description());
    }
    if (arguments() != null) {
      List<PromptArgument> args = PromptArgument.convertSchemaToArguments(arguments());
      prompt.setArguments(args);
    }

    return prompt;
  }

  /**
   * Provides the argument schema builder for the prompt, which defines the expected structure of the arguments to be passed.
   *
   * @return the argument schema builder
   */
  @GenIgnore
  ArraySchemaBuilder arguments();

  /**
   * Provides completion suggestions for a specific argument.
   *
   * @param argument the argument being completed
   * @param context the completion context with previously resolved arguments
   * @return a Future containing completion suggestions, or null if no completion is available
   */
  @GenIgnore
  default Future<Completion> completion(CompletionArgument argument, CompletionContext context) {
    return null;
  }
}
