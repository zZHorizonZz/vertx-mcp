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
import io.vertx.uritemplate.UriTemplate;

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

    // Try dynamic resources
    // Note: Dynamic resource matching requires custom logic since UriTemplate
    // doesn't provide a match() method. This would need to be implemented
    // based on the specific URI template patterns used.

    return Future.succeededFuture(
      JsonResponse.error(request, JsonError.invalidParams("Resource not found: " + uri))
    );
  }

  private Future<JsonResponse> handleListResourceTemplates(JsonRequest request) {
    List<ResourceTemplate> templates = new ArrayList<>();

    // Convert dynamic handlers to resource templates
    // Note: We need to store the template string separately since UriTemplate
    // doesn't provide access to the original template string
    // For now, return empty list until proper template string storage is added
    for (DynamicResourceHandler handler : dynamicHandlers) {
      ResourceTemplate template = new ResourceTemplate()
        .setUriTemplate(handler.getResourceTemplate().toString());
      templates.add(template);
    }

    ListResourceTemplatesResult result = new ListResourceTemplatesResult()
      .setResourceTemplates(templates);

    return Future.succeededFuture(JsonResponse.success(request, result.toJson()));
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

  public void addDynamicResource(UriTemplate template, Supplier<Future<Resource>> handler) {
    dynamicHandlers.add(DynamicResourceHandler.create(template, handler));
  }

  public void addDynamicResource(DynamicResourceHandler handler) {
    dynamicHandlers.add(handler);
  }
}
