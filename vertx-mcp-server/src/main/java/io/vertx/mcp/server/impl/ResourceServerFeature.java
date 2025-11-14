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
        // Extract template variables from the URI
        java.util.Map<String, String> uriParams = extractTemplateVariables(uri, template);

        return handler.apply(uriParams)
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
        .setName(handler.getResourceTemplate())
        .setUriTemplate(handler.getResourceTemplate());
      templates.add(template);
    }

    ListResourceTemplatesResult result = new ListResourceTemplatesResult()
      .setResourceTemplates(templates);

    return Future.succeededFuture(JsonResponse.success(request, result.toJson()));
  }

  /**
   * Utility method to check if a URI matches a URI template pattern.
   * Template variables are denoted by curly braces, e.g., "resource://{id}/details" or "{type}://resource/data"
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

      // Check if this is a complete template variable (e.g., {id})
      if (templatePart.startsWith("{") && templatePart.endsWith("}")) {
        // Variable segment - matches any non-empty value
        if (uriPart.isEmpty()) {
          return false;
        }
        continue;
      }

      // Check if this segment contains template variables (e.g., {type}:)
      if (templatePart.contains("{") && templatePart.contains("}")) {
        if (!matchesSegmentWithVariables(uriPart, templatePart)) {
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

  /**
   * Matches a URI segment against a template segment that contains variables.
   * For example, matches "file:" against "{type}:"
   *
   * @param uriSegment the URI segment to match
   * @param templateSegment the template segment with variables
   * @return true if the URI segment matches the template
   */
  private boolean matchesSegmentWithVariables(String uriSegment, String templateSegment) {
    int templatePos = 0;
    int uriPos = 0;

    while (templatePos < templateSegment.length()) {
      if (templateSegment.charAt(templatePos) == '{') {
        // Find the closing brace
        int closeBrace = templateSegment.indexOf('}', templatePos);
        if (closeBrace == -1) {
          return false; // Malformed template
        }

        // Find what comes after the variable
        int afterVar = closeBrace + 1;
        if (afterVar < templateSegment.length()) {
          // There's literal text after the variable - find it in the URI
          char nextLiteral = templateSegment.charAt(afterVar);
          int nextLiteralPos = uriSegment.indexOf(nextLiteral, uriPos);
          if (nextLiteralPos == -1) {
            return false; // Required literal not found
          }
          // Variable matches everything from uriPos to nextLiteralPos
          if (nextLiteralPos == uriPos) {
            return false; // Variable must match at least one character
          }
          uriPos = nextLiteralPos;
        } else {
          // Variable is at the end - matches rest of URI segment
          if (uriPos >= uriSegment.length()) {
            return false; // Variable must match at least one character
          }
          uriPos = uriSegment.length();
        }
        templatePos = afterVar;
      } else {
        // Literal character - must match
        if (uriPos >= uriSegment.length() || uriSegment.charAt(uriPos) != templateSegment.charAt(templatePos)) {
          return false;
        }
        templatePos++;
        uriPos++;
      }
    }

    // Both must be fully consumed
    return uriPos == uriSegment.length();
  }

  /**
   * Extracts template variables from a URI given a template pattern.
   * For example, given URI "resource://user/123" and template "resource://user/{id}",
   * returns {"id": "123"}
   *
   * @param uri the URI to extract variables from
   * @param template the URI template pattern
   * @return map of variable names to their values
   */
  private java.util.Map<String, String> extractTemplateVariables(String uri, String template) {
    java.util.Map<String, String> variables = new java.util.HashMap<>();

    String[] uriParts = uri.split("/");
    String[] templateParts = template.split("/");

    for (int i = 0; i < templateParts.length && i < uriParts.length; i++) {
      String templatePart = templateParts[i];
      String uriPart = uriParts[i];

      // Check if this is a complete template variable (e.g., {id})
      if (templatePart.startsWith("{") && templatePart.endsWith("}")) {
        String varName = templatePart.substring(1, templatePart.length() - 1);
        variables.put(varName, uriPart);
        continue;
      }

      // Check if this segment contains template variables (e.g., {type}:)
      if (templatePart.contains("{") && templatePart.contains("}")) {
        extractSegmentVariables(uriPart, templatePart, variables);
      }
    }

    return variables;
  }

  /**
   * Extracts variables from a URI segment that matches a template segment with variables.
   * For example, given "file:" and "{type}:", extracts {"type": "file"}
   *
   * @param uriSegment the URI segment
   * @param templateSegment the template segment with variables
   * @param variables map to add extracted variables to
   */
  private void extractSegmentVariables(String uriSegment, String templateSegment, java.util.Map<String, String> variables) {
    int templatePos = 0;
    int uriPos = 0;

    while (templatePos < templateSegment.length()) {
      if (templateSegment.charAt(templatePos) == '{') {
        // Find the closing brace
        int closeBrace = templateSegment.indexOf('}', templatePos);
        if (closeBrace == -1) {
          return; // Malformed template
        }

        String varName = templateSegment.substring(templatePos + 1, closeBrace);

        // Find what comes after the variable
        int afterVar = closeBrace + 1;
        if (afterVar < templateSegment.length()) {
          // There's literal text after the variable - find it in the URI
          char nextLiteral = templateSegment.charAt(afterVar);
          int nextLiteralPos = uriSegment.indexOf(nextLiteral, uriPos);
          if (nextLiteralPos != -1) {
            // Extract variable value
            String value = uriSegment.substring(uriPos, nextLiteralPos);
            variables.put(varName, value);
            uriPos = nextLiteralPos;
          }
        } else {
          // Variable is at the end - extract rest of URI segment
          String value = uriSegment.substring(uriPos);
          variables.put(varName, value);
          uriPos = uriSegment.length();
        }
        templatePos = afterVar;
      } else {
        // Literal character - skip it
        templatePos++;
        uriPos++;
      }
    }
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

  public void addDynamicResource(String template, java.util.function.Function<java.util.Map<String, String>, Future<Resource>> handler) {
    dynamicHandlers.add(DynamicResourceHandler.create(template, handler));
  }

  public void addDynamicResource(DynamicResourceHandler handler) {
    dynamicHandlers.add(handler);
  }

  /**
   * Clears all registered resource handlers.
   * Useful for test isolation when reusing feature instances.
   */
  public void clear() {
    staticHandlers.clear();
    dynamicHandlers.clear();
  }
}
