package io.vertx.mcp.server.feature;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.mcp.common.content.Content;
import io.vertx.mcp.common.request.CallToolRequest;
import io.vertx.mcp.common.result.CallToolResult;
import io.vertx.mcp.common.result.ListToolsResult;
import io.vertx.mcp.common.rpc.JsonError;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.common.tool.Tool;
import io.vertx.mcp.server.ServerRequest;
import io.vertx.mcp.server.StructuredToolHandler;
import io.vertx.mcp.server.UnstructuredToolHandler;
import io.vertx.mcp.server.impl.ServerFeatureBase;
import io.vertx.mcp.server.impl.ServerFeatureStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * The ToolServerFeature class implements the ServerFeatureBase and provides functionality to handle JSON-RPC requests related to tool management. This includes listing available
 * tools, calling tools, and managing structured and unstructured tool handlers.
 *
 * @version 2025-06-18
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/server/tools">Server Features - Tools</a>
 */
public class ToolServerFeature extends ServerFeatureBase {

  private final ServerFeatureStorage<StructuredToolHandler> structuredTools;
  private final ServerFeatureStorage<UnstructuredToolHandler> unstructuredTools;
  private Vertx vertx;

  public ToolServerFeature() {
    this.structuredTools = new ServerFeatureStorage<>(() -> vertx, "notifications/tools/list_changed");
    this.unstructuredTools = new ServerFeatureStorage<>(() -> vertx, "notifications/tools/list_changed");
  }

  @Override
  public void init(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public Map<String, BiFunction<ServerRequest, JsonRequest, Future<JsonResponse>>> getHandlers() {
    return Map.of(
      "tools/list", this::handleListTools,
      "tools/call", this::handleCallTool
    );
  }

  private Future<JsonResponse> handleListTools(ServerRequest serverRequest, JsonRequest request) {
    List<Tool> toolsList = new ArrayList<>();

    for (StructuredToolHandler handler : structuredTools.values()) {
      toolsList.add(handler.toFeature());
    }

    for (UnstructuredToolHandler handler : unstructuredTools.values()) {
      toolsList.add(handler.toFeature());
    }

    ListToolsResult result = new ListToolsResult().setTools(toolsList);
    return Future.succeededFuture(JsonResponse.success(request, result.toJson()));
  }

  private Future<JsonResponse> handleCallTool(ServerRequest serverRequest, JsonRequest request) {
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

    // Ensure arguments is not null
    if (arguments == null) {
      arguments = new JsonObject();
    }

    // Check structured tools first
    StructuredToolHandler structuredHandler = structuredTools.get(toolName);
    if (structuredHandler != null) {
      return executeStructuredTool(structuredHandler, arguments).map(result -> result.toResponse(request));
    }

    // Check unstructured tools
    UnstructuredToolHandler unstructuredHandler = unstructuredTools.get(toolName);
    if (unstructuredHandler != null) {
      return executeUnstructuredTool(unstructuredHandler, arguments).map(result -> result.toResponse(request));
    }

    return Future.succeededFuture(
      JsonResponse.error(request, JsonError.invalidParams("Tool not found: " + toolName))
    );
  }

  private Future<CallToolResult> executeStructuredTool(StructuredToolHandler handler, JsonObject arguments) {
    return handler.apply(arguments)
      .compose(result -> Future.succeededFuture(new CallToolResult().setStructuredContent(result).setIsError(false)))
      .recover(err -> Future.succeededFuture(new CallToolResult()
        .setContent(new JsonArray().add(new JsonObject().put("type", "text").put("text", "Error: " + err.getMessage())))
        .setIsError(true))
      );
  }

  private Future<CallToolResult> executeUnstructuredTool(UnstructuredToolHandler handler, JsonObject arguments) {
    return handler.apply(arguments)
      .compose(contents -> {
        JsonArray contentArray = new JsonArray();
        for (Content content : contents) {
          contentArray.add(content.toJson());
        }

        return Future.succeededFuture(new CallToolResult().setContent(contentArray).setIsError(false));
      })
      .recover(err -> Future.succeededFuture(new CallToolResult()
        .setIsError(true)
        .setContent(new JsonArray().add(new JsonObject().put("type", "text").put("text", "Error: " + err.getMessage()))))
      );
  }

  /**
   * Adds a structured tool handler with just name and schema information.
   *
   * @param name the tool name
   * @param inputSchema the input schema
   * @param outputSchema the output schema
   * @param handler the handler function
   */
  public void addStructuredTool(String name, ObjectSchemaBuilder inputSchema, ObjectSchemaBuilder outputSchema, Function<JsonObject, Future<JsonObject>> handler) {
    this.addStructuredTool(name, null, null, inputSchema, outputSchema, handler);
  }

  /**
   * Adds a structured tool handler with name, title, and schema information.
   *
   * @param name the tool name
   * @param title the tool title (optional)
   * @param inputSchema the input schema
   * @param outputSchema the output schema
   * @param handler the handler function
   */
  public void addStructuredTool(String name, String title, ObjectSchemaBuilder inputSchema, ObjectSchemaBuilder outputSchema, Function<JsonObject, Future<JsonObject>> handler) {
    this.addStructuredTool(name, title, null, inputSchema, outputSchema, handler);
  }

  /**
   * Adds a structured tool handler with full metadata.
   *
   * @param name the tool name
   * @param title the tool title (optional)
   * @param description the tool description (optional)
   * @param inputSchema the input schema
   * @param outputSchema the output schema
   * @param handler the handler function
   */
  public void addStructuredTool(String name, String title, String description, ObjectSchemaBuilder inputSchema, ObjectSchemaBuilder outputSchema,
    Function<JsonObject, Future<JsonObject>> handler) {
    this.addStructuredTool(StructuredToolHandler.create(name, title, description, inputSchema, outputSchema, handler));
  }

  /**
   * Adds a structured tool handler.
   *
   * @param handler the structured tool handler
   * @throws IllegalArgumentException if the handler is null or has an invalid name
   */
  public void addStructuredTool(StructuredToolHandler handler) {
    if (handler == null) {
      throw new IllegalArgumentException("Handler must not be null");
    }
    if (handler.name() == null || handler.name().isEmpty()) {
      throw new IllegalArgumentException("Tool name must not be null or empty");
    }

    structuredTools.put(handler.name(), handler);
  }

  /**
   * Adds an unstructured tool handler with just name and schema information.
   *
   * @param name the tool name
   * @param inputSchema the input schema
   * @param handler the handler function
   */
  public void addUnstructuredTool(String name, ObjectSchemaBuilder inputSchema, Function<JsonObject, Future<Content[]>> handler) {
    this.addUnstructuredTool(name, null, null, inputSchema, handler);
  }

  /**
   * Adds an unstructured tool handler with name, title, and schema information.
   *
   * @param name the tool name
   * @param title the tool title (optional)
   * @param inputSchema the input schema
   * @param handler the handler function
   */
  public void addUnstructuredTool(String name, String title, ObjectSchemaBuilder inputSchema, Function<JsonObject, Future<Content[]>> handler) {
    this.addUnstructuredTool(name, title, null, inputSchema, handler);
  }

  /**
   * Adds an unstructured tool handler with full metadata.
   *
   * @param name the tool name
   * @param title the tool title (optional)
   * @param description the tool description (optional)
   * @param inputSchema the input schema
   * @param handler the handler function
   */
  public void addUnstructuredTool(String name, String title, String description, ObjectSchemaBuilder inputSchema, Function<JsonObject, Future<Content[]>> handler) {
    this.addUnstructuredTool(UnstructuredToolHandler.create(name, title, description, inputSchema, handler));
  }

  /**
   * Adds an unstructured tool handler.
   *
   * @param handler the unstructured tool handler
   * @throws IllegalArgumentException if the handler is null or has an invalid name
   */
  public void addUnstructuredTool(UnstructuredToolHandler handler) {
    if (handler == null) {
      throw new IllegalArgumentException("Handler must not be null");
    }
    if (handler.name() == null || handler.name().isEmpty()) {
      throw new IllegalArgumentException("Tool name must not be null or empty");
    }

    unstructuredTools.put(handler.name(), handler);
  }

  /**
   * Retrieves a list of structured tool handlers managed by this feature.
   *
   * @return a list of {@link StructuredToolHandler} instances representing the structured tools.
   */
  public List<StructuredToolHandler> structuredTools() {
    return new ArrayList<>(this.structuredTools.values());
  }

  /**
   * Retrieves a list of unstructured tool handlers managed by this feature.
   *
   * @return a list of {@link UnstructuredToolHandler} instances representing the unstructured tools.
   */
  public List<UnstructuredToolHandler> unstructuredTools() {
    return new ArrayList<>(this.unstructuredTools.values());
  }
}
