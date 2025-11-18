package io.vertx.mcp.server.impl;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.common.dsl.ArraySchemaBuilder;
import io.vertx.mcp.common.prompt.Prompt;
import io.vertx.mcp.common.prompt.PromptArgument;
import io.vertx.mcp.common.prompt.PromptMessage;
import io.vertx.mcp.common.request.GetPromptRequest;
import io.vertx.mcp.common.result.GetPromptResult;
import io.vertx.mcp.common.result.ListPromptsResult;
import io.vertx.mcp.common.rpc.JsonError;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.server.PromptHandler;
import io.vertx.mcp.server.ServerFeature;
import io.vertx.mcp.server.ServerRequest;

import java.util.*;
import java.util.function.Function;

public class PromptServerFeature implements ServerFeature {

  private final Map<String, PromptHandler> prompts = new HashMap<>();

  @Override
  public void handle(ServerRequest serverRequest) {
    // Retrieve the parsed JSON-RPC request from the ServerRequest
    JsonRequest request = serverRequest.getJsonRequest();

    if (request == null) {
      serverRequest.response().end(
        new JsonResponse(JsonError.internalError("No JSON-RPC request found"), null)
      );
      return;
    }

    String method = request.getMethod();

    Future<JsonResponse> responseFuture;
    switch (method) {
      case "prompts/list":
        responseFuture = handleListPrompts(request);
        break;
      case "prompts/get":
        responseFuture = handleGetPrompt(request);
        break;
      default:
        responseFuture = Future.succeededFuture(
          JsonResponse.error(request, JsonError.methodNotFound(method))
        );
        break;
    }

    responseFuture.onComplete(ar -> {
      if (ar.succeeded()) {
        serverRequest.response().end(ar.result());
      } else {
        serverRequest.response().end(
          JsonResponse.error(request, JsonError.internalError(ar.cause().getMessage()))
        );
      }
    });
  }

  private Future<JsonResponse> handleListPrompts(JsonRequest request) {
    List<Prompt> promptsList = new ArrayList<>();

    // Build Prompt objects from registered prompts
    for (Map.Entry<String, PromptHandler> entry : prompts.entrySet()) {
      PromptHandler handler = entry.getValue();
      Prompt prompt = new Prompt().setName(entry.getKey());

      // Add optional fields if present
      if (handler.title() != null) {
        prompt.setTitle(handler.title());
      }
      if (handler.description() != null) {
        prompt.setDescription(handler.description());
      }

      if (handler.arguments() != null) {
        List<PromptArgument> arguments = convertSchemaToArguments(handler.arguments());
        prompt.setArguments(arguments);
      }

      promptsList.add(prompt);
    }

    ListPromptsResult result = new ListPromptsResult().setPrompts(promptsList);

    // TODO: Handle cursor/pagination if needed

    return Future.succeededFuture(JsonResponse.success(request, result.toJson()));
  }

  private Future<JsonResponse> handleGetPrompt(JsonRequest request) {
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

  @Override
  public Set<String> getCapabilities() {
    return Set.of("prompts/list", "prompts/get");
  }

  public void addPrompt(String name, ArraySchemaBuilder arguments, Function<JsonObject, Future<List<PromptMessage>>> handler) {
    this.addPrompt(name, null, null, arguments, handler);
  }

  public void addPrompt(String name, String title, ArraySchemaBuilder arguments, Function<JsonObject, Future<List<PromptMessage>>> handler) {
    this.addPrompt(name, title, null, arguments, handler);
  }

  public void addPrompt(String name, String title, String description, ArraySchemaBuilder arguments, Function<JsonObject, Future<List<PromptMessage>>> handler) {
    this.addPrompt(PromptHandler.create(name, title, description, arguments, handler));
  }

  /**
   * Adds a prompt handler.
   *
   * @param handler the prompt handler
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
   * Removes a prompt by name.
   *
   * @param name the prompt name
   * @return true if the prompt was removed, false if it didn't exist
   */
  public boolean removePrompt(String name) {
    return prompts.remove(name) != null;
  }

  /**
   * Checks if a prompt is registered.
   *
   * @param name the prompt name
   * @return true if the prompt exists
   */
  public boolean hasPrompt(String name) {
    return prompts.containsKey(name);
  }

  /**
   * Gets the names of all registered prompts.
   *
   * @return set of prompt names
   */
  public Set<String> getPromptNames() {
    return prompts.keySet();
  }

  /**
   * Converts an array schema to a list of PromptArgument objects. The schema should be an array schema with object items containing properties.
   *
   * @param schemaBuilder the schema builder (array schema)
   * @return List of PromptArgument objects
   */
  private List<PromptArgument> convertSchemaToArguments(io.vertx.json.schema.common.dsl.SchemaBuilder schemaBuilder) {
    List<PromptArgument> argumentsList = new ArrayList<>();

    try {
      // Convert schema to JSON
      JsonObject schemaJson = schemaBuilder.toJson();

      // Get the items field (should be an object schema)
      JsonObject itemsSchema = schemaJson.getJsonObject("items");
      if (itemsSchema == null) {
        return argumentsList;
      }

      // Get the properties from the object schema
      JsonObject properties = itemsSchema.getJsonObject("properties");
      if (properties == null) {
        return argumentsList;
      }

      // Get the required fields list
      JsonArray requiredFields = itemsSchema.getJsonArray("required");
      List<String> requiredList = new ArrayList<>();
      if (requiredFields != null) {
        for (int i = 0; i < requiredFields.size(); i++) {
          requiredList.add(requiredFields.getString(i));
        }
      }

      // Create PromptArgument for each property
      for (String propertyName : properties.fieldNames()) {
        JsonObject propertySchema = properties.getJsonObject(propertyName);

        PromptArgument argument = new PromptArgument()
          .setName(propertyName)
          .setRequired(requiredList.contains(propertyName));

        // Extract description if available
        if (propertySchema.containsKey("description")) {
          argument.setDescription(propertySchema.getString("description"));
        }

        // Extract title if available
        if (propertySchema.containsKey("title")) {
          argument.setTitle(propertySchema.getString("title"));
        }

        argumentsList.add(argument);
      }
    } catch (Exception e) {
      // If schema conversion fails, return empty list
      System.err.println("Failed to convert schema to arguments: " + e.getMessage());
    }

    return argumentsList;
  }

  /**
   * Clears all registered prompts. Useful for test isolation when reusing feature instances.
   */
  public void clear() {
    prompts.clear();
  }
}
