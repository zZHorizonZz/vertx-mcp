package io.vertx.mcp.server.impl;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.resources.Resource;
import io.vertx.mcp.common.resources.ResourceTemplate;
import io.vertx.mcp.common.result.ListResourceTemplatesResult;
import io.vertx.mcp.common.result.ListResourcesResult;
import io.vertx.mcp.common.result.ReadResourceResult;
import io.vertx.mcp.common.rpc.JsonError;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.server.DynamicResourceHandler;
import io.vertx.mcp.server.ServerFeature;
import io.vertx.mcp.server.ServerRequest;
import io.vertx.mcp.server.StaticResourceHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class ResourceServerFeature implements ServerFeature {

  private final List<StaticResourceHandler> staticHandlers = new ArrayList<>();
  private final List<DynamicResourceHandler> dynamicHandlers = new ArrayList<>();

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
      case "resources/list":
        responseFuture = handleListResources(request);
        break;
      case "resources/read":
        responseFuture = handleReadResource(request);
        break;
      case "resources/templates/list":
        responseFuture = handleListResourceTemplates(request);
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

  private Future<JsonResponse> handleListResources(JsonRequest request) {
    JsonArray resourcesArray = new JsonArray();

    // Add all static resources
    for (StaticResourceHandler handler : staticHandlers) {
      JsonObject resourceInfo = new JsonObject()
        .put("uri", "resource://" + handler.getResourceName())
        .put("name", handler.getResourceName());
      resourcesArray.add(resourceInfo);
    }

    ListResourcesResult result = new ListResourcesResult()
      .setResources(resourcesArray);

    return Future.succeededFuture(JsonResponse.success(request, result.toJson()));
  }

  private Future<JsonResponse> handleReadResource(JsonRequest request) {
    JsonObject params = request.getNamedParams();
    if (params == null || !params.containsKey("uri")) {
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.invalidParams("Missing 'uri' parameter"))
      );
    }

    String uri = params.getString("uri");

    // Try static resources first
    for (StaticResourceHandler handler : staticHandlers) {
      String staticUri = "resource://" + handler.getResourceName();
      if (staticUri.equals(uri)) {
        return handler.get()
          .compose(resource -> {
            JsonArray contents = new JsonArray().add(resource.toJson());
            ReadResourceResult result = new ReadResourceResult().setContents(contents);
            return Future.succeededFuture(JsonResponse.success(request, result.toJson()));
          })
          .recover(err -> Future.succeededFuture(
            JsonResponse.error(request, JsonError.internalError(err.getMessage()))
          ));
      }
    }

    // Try dynamic resources - match using template pattern
    for (DynamicResourceHandler handler : dynamicHandlers) {
      String template = handler.getResourceTemplate();

      if (matchesTemplate(uri, template)) {
        return handler.get()
          .compose(resource -> {
            JsonArray contents = new JsonArray().add(resource.toJson());
            ReadResourceResult result = new ReadResourceResult().setContents(contents);
            return Future.succeededFuture(JsonResponse.success(request, result.toJson()));
          })
          .recover(err -> Future.succeededFuture(
            JsonResponse.error(request, JsonError.internalError(err.getMessage()))
          ));
      }
    }

    return Future.succeededFuture(
      JsonResponse.error(request, JsonError.invalidParams("Resource not found: " + uri))
    );
  }

  private Future<JsonResponse> handleListResourceTemplates(JsonRequest request) {
    List<ResourceTemplate> templates = new ArrayList<>();

    // Convert dynamic handlers to resource templates
    for (DynamicResourceHandler handler : dynamicHandlers) {
      ResourceTemplate template = new ResourceTemplate()
        .setUriTemplate(handler.getResourceTemplate());
      templates.add(template);
    }

    ListResourceTemplatesResult result = new ListResourceTemplatesResult()
      .setResourceTemplates(templates);

    return Future.succeededFuture(JsonResponse.success(request, result.toJson()));
  }

  /**
   * Utility method to check if a URI matches a URI template pattern.
   * Template variables are denoted by curly braces, e.g., "resource://{id}/details"
   *
   * @param uri the URI to match
   * @param template the URI template pattern
   * @return true if the URI matches the template pattern
   */
  private boolean matchesTemplate(String uri, String template) {
    String[] uriParts = uri.split("/");
    String[] templateParts = template.split("/");

    // Must have same number of segments
    if (uriParts.length != templateParts.length) {
      return false;
    }

    for (int i = 0; i < templateParts.length; i++) {
      String templatePart = templateParts[i];
      String uriPart = uriParts[i];

      // Check if this is a template variable (e.g., {id})
      if (templatePart.startsWith("{") && templatePart.endsWith("}")) {
        // Variable segment - matches any non-empty value
        if (uriPart.isEmpty()) {
          return false;
        }
        continue;
      }

      // Literal segment - must match exactly
      if (!templatePart.equals(uriPart)) {
        return false;
      }
    }

    return true;
  }

  @Override
  public Set<String> getCapabilities() {
    return Set.of("resources/list", "resources/read", "resources/templates/list");
  }

  public void addStaticResource(String name, Supplier<Future<Resource>> resourceSupplier) {
    staticHandlers.add(StaticResourceHandler.create(name, resourceSupplier));
  }

  public void addStaticResource(StaticResourceHandler handler) {
    staticHandlers.add(handler);
  }

  public void addDynamicResource(String template, Supplier<Future<Resource>> handler) {
    dynamicHandlers.add(DynamicResourceHandler.create(template, handler));
  }

  public void addDynamicResource(DynamicResourceHandler handler) {
    dynamicHandlers.add(handler);
  }
}
