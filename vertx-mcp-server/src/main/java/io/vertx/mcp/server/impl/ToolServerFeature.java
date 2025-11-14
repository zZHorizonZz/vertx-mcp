package io.vertx.mcp.server.impl;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.common.dsl.Schemas;
import io.vertx.mcp.common.content.Content;
import io.vertx.mcp.common.request.CallToolRequest;
import io.vertx.mcp.common.result.CallToolResult;
import io.vertx.mcp.common.result.ListToolsResult;
import io.vertx.mcp.common.rpc.JsonError;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.common.tool.Tool;
import io.vertx.mcp.server.ServerFeature;
import io.vertx.mcp.server.ServerRequest;
import io.vertx.mcp.server.StructuredToolHandler;
import io.vertx.mcp.server.UnstructuredToolHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ToolServerFeature implements ServerFeature {

  private final Map<String, ToolRegistration> tools = new HashMap<>();

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
      case "tools/list":
        responseFuture = handleListTools(request);
        break;
      case "tools/call":
        responseFuture = handleCallTool(request);
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

  private Future<JsonResponse> handleListTools(JsonRequest request) {
    List<Tool> toolsList = new ArrayList<>();

    // Build Tool objects from registered tools
    for (Map.Entry<String, ToolRegistration> entry : tools.entrySet()) {
      ToolRegistration registration = entry.getValue();
      Tool tool = new Tool()
        .setName(entry.getKey())
        .setInputSchema(registration.inputSchema.toJson());

      // Add optional fields if present
      if (registration.description != null) {
        tool.setDescription(registration.description);
      }
      if (registration.title != null) {
        tool.setTitle(registration.title);
      }
      if (registration.outputSchema != null) {
        tool.setOutputSchema(registration.outputSchema.toJson());
      }

      toolsList.add(tool);
    }

    ListToolsResult result = new ListToolsResult().setTools(toolsList);
    return Future.succeededFuture(JsonResponse.success(request, result.toJson()));
  }

  private Future<JsonResponse> handleCallTool(JsonRequest request) {
    // Parse the request parameters
    JsonObject params = request.getNamedParams();
    if (params == null) {
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.invalidParams("Missing parameters"))
      );
    }

    CallToolRequest callRequest = new CallToolRequest(params);
    String toolName = callRequest.getName();
    JsonObject arguments = callRequest.getArguments();

    if (toolName == null || toolName.isEmpty()) {
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.invalidParams("Missing 'name' parameter"))
      );
    }

    // Find the tool registration
    ToolRegistration registration = tools.get(toolName);
    if (registration == null) {
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.invalidParams("Tool not found: " + toolName))
      );
    }

    // Ensure arguments is not null
    if (arguments == null) {
      arguments = new JsonObject();
    }

    // Execute the appropriate handler
    if (registration.structuredHandler != null) {
      return executeStructuredTool(request, registration.structuredHandler, arguments);
    } else if (registration.unstructuredHandler != null) {
      return executeUnstructuredTool(request, registration.unstructuredHandler, arguments);
    } else {
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.internalError("No handler registered for tool: " + toolName))
      );
    }
  }

  private Future<JsonResponse> executeStructuredTool(JsonRequest request, StructuredToolHandler handler, JsonObject arguments) {
    return handler.apply(arguments)
      .compose(result -> {
        CallToolResult callResult = new CallToolResult()
          .setStructuredContent(result)
          .setIsError(false);
        return Future.succeededFuture(JsonResponse.success(request, callResult.toJson()));
      })
      .recover(err -> {
        CallToolResult callResult = new CallToolResult()
          .setIsError(true);

        // Create error content
        JsonArray errorContent = new JsonArray()
          .add(new JsonObject()
            .put("type", "text")
            .put("text", "Error: " + err.getMessage()));
        callResult.setContent(errorContent);

        return Future.succeededFuture(JsonResponse.success(request, callResult.toJson()));
      });
  }

  private Future<JsonResponse> executeUnstructuredTool(JsonRequest request, UnstructuredToolHandler handler, JsonObject arguments) {
    return handler.apply(arguments)
      .compose(contents -> {
        JsonArray contentArray = new JsonArray();
        for (Content content : contents) {
          contentArray.add(content.toJson());
        }

        CallToolResult callResult = new CallToolResult()
          .setContent(contentArray)
          .setIsError(false);
        return Future.succeededFuture(JsonResponse.success(request, callResult.toJson()));
      })
      .recover(err -> {
        CallToolResult callResult = new CallToolResult()
          .setIsError(true);

        // Create error content
        JsonArray errorContent = new JsonArray()
          .add(new JsonObject()
            .put("type", "text")
            .put("text", "Error: " + err.getMessage()));
        callResult.setContent(errorContent);

        return Future.succeededFuture(JsonResponse.success(request, callResult.toJson()));
      });
  }

  @Override
  public Set<String> getCapabilities() {
    return Set.of("tools/list", "tools/call");
  }

  /**
   * Adds a structured tool handler.
   *
   * @param name the tool name
   * @param handler the structured tool handler
   */
  public void addStructuredTool(String name, StructuredToolHandler handler) {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("Tool name must not be null or empty");
    }
    if (handler == null) {
      throw new IllegalArgumentException("Handler must not be null");
    }

    ToolRegistration registration = new ToolRegistration();
    registration.inputSchema = handler.inputSchema();
    registration.outputSchema = handler.outputSchema();
    registration.structuredHandler = handler;

    tools.put(name, registration);
  }

  /**
   * Adds a structured tool handler with additional metadata.
   *
   * @param name the tool name
   * @param title the tool title (optional)
   * @param description the tool description (optional)
   * @param handler the structured tool handler
   */
  public void addStructuredTool(String name, String title, String description, StructuredToolHandler handler) {
    addStructuredTool(name, handler);
    ToolRegistration registration = tools.get(name);
    registration.title = title;
    registration.description = description;
  }

  /**
   * Adds an unstructured tool handler.
   *
   * @param name the tool name
   * @param handler the unstructured tool handler
   */
  public void addUnstructuredTool(String name, UnstructuredToolHandler handler) {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("Tool name must not be null or empty");
    }
    if (handler == null) {
      throw new IllegalArgumentException("Handler must not be null");
    }

    ToolRegistration registration = new ToolRegistration();
    registration.inputSchema = handler.inputSchema();
    registration.unstructuredHandler = handler;

    tools.put(name, registration);
  }

  /**
   * Adds an unstructured tool handler with additional metadata.
   *
   * @param name the tool name
   * @param title the tool title (optional)
   * @param description the tool description (optional)
   * @param handler the unstructured tool handler
   */
  public void addUnstructuredTool(String name, String title, String description, UnstructuredToolHandler handler) {
    addUnstructuredTool(name, handler);
    ToolRegistration registration = tools.get(name);
    registration.title = title;
    registration.description = description;
  }

  /**
   * Removes a tool by name.
   *
   * @param name the tool name
   * @return true if the tool was removed, false if it didn't exist
   */
  public boolean removeTool(String name) {
    return tools.remove(name) != null;
  }

  /**
   * Checks if a tool is registered.
   *
   * @param name the tool name
   * @return true if the tool exists
   */
  public boolean hasTool(String name) {
    return tools.containsKey(name);
  }

  /**
   * Gets the names of all registered tools.
   *
   * @return set of tool names
   */
  public Set<String> getToolNames() {
    return tools.keySet();
  }

  /**
   * Clears all registered tools.
   * Useful for test isolation when reusing feature instances.
   */
  public void clear() {
    tools.clear();
  }

  /**
   * Internal class to hold tool registration information.
   */
  private static class ToolRegistration {
    io.vertx.json.schema.common.dsl.SchemaBuilder inputSchema;
    io.vertx.json.schema.common.dsl.SchemaBuilder outputSchema;

    String title;
    String description;
    StructuredToolHandler structuredHandler;
    UnstructuredToolHandler unstructuredHandler;
  }
}
