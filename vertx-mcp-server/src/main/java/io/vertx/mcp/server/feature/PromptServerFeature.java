package io.vertx.mcp.server.feature;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.common.dsl.ArraySchemaBuilder;
import io.vertx.mcp.common.completion.Completion;
import io.vertx.mcp.common.completion.CompletionArgument;
import io.vertx.mcp.common.completion.CompletionContext;
import io.vertx.mcp.common.prompt.Prompt;
import io.vertx.mcp.common.prompt.PromptMessage;
import io.vertx.mcp.common.request.GetPromptRequest;
import io.vertx.mcp.common.result.GetPromptResult;
import io.vertx.mcp.common.result.ListPromptsResult;
import io.vertx.mcp.common.rpc.JsonError;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.server.CompletionProvider;
import io.vertx.mcp.server.PromptHandler;
import io.vertx.mcp.server.ServerRequest;
import io.vertx.mcp.server.impl.ServerFeatureBase;
import io.vertx.mcp.server.impl.ServerFeatureStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * The PromptServerFeature class implements the ServerFeature interface and provides functionality to handle JSON-RPC requests related to prompt management. This includes listing
 * available prompts, retrieving prompt details, and registering/removing prompt handlers.
 *
 * @version 2025-06-18
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/server/prompts">Server Features - Prompts</a>
 */
public class PromptServerFeature extends ServerFeatureBase implements CompletionProvider {

  private final ServerFeatureStorage<PromptHandler> prompts;
  private Vertx vertx;

  public PromptServerFeature() {
    this.prompts = new ServerFeatureStorage<>(() -> vertx, "notifications/prompts/list_changed");
  }

  @Override
  public void init(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public Map<String, BiFunction<ServerRequest, JsonRequest, Future<JsonResponse>>> getHandlers() {
    return Map.of(
      "prompts/list", this::handleListPrompts,
      "prompts/get", this::handleGetPrompt
    );
  }

  @Override
  public Future<Completion> handleCompletion(String refType, String refName, CompletionArgument argument, CompletionContext context) {
    // Find the prompt
    PromptHandler handler = prompts.get(refName);
    if (handler == null) {
      // Return empty completion for unknown prompt
      return Future.succeededFuture(new Completion()
        .setValues(new ArrayList<>())
        .setTotal(0)
        .setHasMore(false));
    }

    // Use the handler's completion function
    Future<Completion> completionFuture = handler.completion(argument, context);
    if (completionFuture != null) {
      return completionFuture;
    }

    // Default: return empty completion
    return Future.succeededFuture(new Completion()
      .setValues(new ArrayList<>())
      .setTotal(0)
      .setHasMore(false));
  }

  @Override
  public Set<String> getCompletionCapabilities() {
    return Set.of("ref/prompt");
  }

  private Future<JsonResponse> handleListPrompts(ServerRequest serverRequest, JsonRequest request) {
    List<Prompt> promptsList = new ArrayList<>();

    for (PromptHandler handler : prompts.values()) {
      promptsList.add(handler.toFeature());
    }

    ListPromptsResult result = new ListPromptsResult().setPrompts(promptsList);

    // TODO: Handle cursor/pagination

    return Future.succeededFuture(JsonResponse.success(request, result.toJson()));
  }

  private Future<JsonResponse> handleGetPrompt(ServerRequest serverRequest, JsonRequest request) {
    // Parse the request parameters
    JsonObject params = request.getNamedParams();
    if (params == null) {
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.invalidParams("Missing parameters"))
      );
    }

    GetPromptRequest getRequest = new GetPromptRequest(params);
    String promptName = getRequest.getName();
    JsonObject arguments = getRequest.getArguments();

    if (promptName == null || promptName.isEmpty()) {
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.invalidParams("Missing 'name' parameter"))
      );
    }

    // Find the prompt registration
    PromptHandler registration = prompts.get(promptName);
    if (registration == null) {
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.invalidParams("Prompt not found: " + promptName))
      );
    }

    // Ensure arguments is not null
    if (arguments == null) {
      arguments = new JsonObject();
    }

    // Execute the handler
    return executePrompt(request, registration, arguments);
  }

  private Future<JsonResponse> executePrompt(JsonRequest request, PromptHandler handler, JsonObject arguments) {
    return handler.apply(arguments).compose(messages -> {
      GetPromptResult result = new GetPromptResult().setMessages(messages);

      if (handler.description() != null) {
        result.setDescription(handler.description());
      }

      return Future.succeededFuture(JsonResponse.success(request, result.toJson()));
    }).recover(err -> Future.succeededFuture(
      JsonResponse.error(request, JsonError.internalError(err.getMessage()))
    ));
  }

  /**
   * Adds a new prompt to the server with the given name, arguments schema, and handler. The added prompt will have no title or description.
   *
   * @param name the unique name of the prompt
   * @param arguments the schema builder defining the arguments expected by the prompt
   * @param handler the handler function responsible for processing the prompt request and returning a list of {@link PromptMessage}
   */
  public void addPrompt(String name, ArraySchemaBuilder arguments, Function<JsonObject, Future<List<PromptMessage>>> handler) {
    this.addPrompt(name, null, null, arguments, handler);
  }

  /**
   * Adds a prompt to the feature with the specified name, title, arguments schema, and a handler for processing the prompt.
   *
   * @param name the unique name of the prompt
   * @param title the title of the prompt, providing a short description
   * @param arguments the schema defining the expected structure of the prompt's arguments
   * @param handler a function to handle the prompt logic, receiving a {@link JsonObject} as input and returning a {@link Future} of a list of {@link PromptMessage} objects
   */
  public void addPrompt(String name, String title, ArraySchemaBuilder arguments, Function<JsonObject, Future<List<PromptMessage>>> handler) {
    this.addPrompt(name, title, null, arguments, handler);
  }

  /**
   * Adds a prompt to the server with the specified parameters.
   *
   * @param name the name of the prompt
   * @param title the title of the prompt
   * @param description a description of the prompt
   * @param arguments the schema builder defining the expected structure of arguments for the prompt
   * @param handler the function to process the input JSON and return a future list of prompt messages
   */
  public void addPrompt(String name, String title, String description, ArraySchemaBuilder arguments, Function<JsonObject, Future<List<PromptMessage>>> handler) {
    this.addPrompt(PromptHandler.create(name, title, description, arguments, handler));
  }

  /**
   * Adds a new prompt handler to the collection of prompts. The handler must provide a valid name that is non-null and non-empty. If the handler's name is null or empty, or if the
   * handler itself is null, an exception will be thrown.
   *
   * @param handler the prompt handler to be added, which must provide a valid non-null and non-empty name
   * @throws IllegalArgumentException if the handler is null or if the handler's name is null or empty
   */
  public void addPrompt(PromptHandler handler) {
    if (handler == null) {
      throw new IllegalArgumentException("Handler must not be null");
    }

    if (handler.name() == null || handler.name().isEmpty()) {
      throw new IllegalArgumentException("Prompt name must not be null or empty");
    }

    prompts.put(handler.name(), handler);
  }

  /**
   * Retrieves a list of prompt handlers managed by this feature.
   *
   * @return a list of {@link PromptHandler} instances representing the prompts.
   */
  public List<PromptHandler> prompts() {
    return new ArrayList<>(this.prompts.values());
  }
}
