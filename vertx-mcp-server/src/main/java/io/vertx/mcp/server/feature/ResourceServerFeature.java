package io.vertx.mcp.server.feature;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.completion.Completion;
import io.vertx.mcp.common.completion.CompletionArgument;
import io.vertx.mcp.common.completion.CompletionContext;
import io.vertx.mcp.common.resources.Resource;
import io.vertx.mcp.common.resources.ResourceTemplate;
import io.vertx.mcp.common.result.ListResourceTemplatesResult;
import io.vertx.mcp.common.result.ListResourcesResult;
import io.vertx.mcp.common.result.ReadResourceResult;
import io.vertx.mcp.common.rpc.JsonError;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.server.DynamicResourceHandler;
import io.vertx.mcp.server.ServerRequest;
import io.vertx.mcp.server.StaticResourceHandler;
import io.vertx.mcp.server.impl.ServerFeatureBase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The ResourceServerFeature class implements the ServerFeatureBase and provides functionality to handle JSON-RPC requests related to resource management. This includes listing
 * available resources, reading resource content, and managing static and dynamic resource handlers.
 *
 * @version 2025-06-18
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/server/resources">Server Features - Resources</a>
 */
public class ResourceServerFeature extends ServerFeatureBase {

  private final List<StaticResourceHandler> staticHandlers = new ArrayList<>();
  private final List<DynamicResourceHandler> dynamicHandlers = new ArrayList<>();

  @Override
  public Map<String, BiFunction<ServerRequest, JsonRequest, Future<JsonResponse>>> getHandlers() {
    return Map.of(
      "resources/list", this::handleListResources,
      "resources/read", this::handleReadResource,
      "resources/templates/list", this::handleListResourceTemplates
    );
  }

  private Future<JsonResponse> handleListResources(ServerRequest serverRequest, JsonRequest request) {
    JsonArray resources = new JsonArray();

    // Add all static resources
    for (StaticResourceHandler handler : staticHandlers) {
      JsonObject resource = new JsonObject().put("uri", handler.uri());

      if (handler.name() != null) {
        resource.put("name", handler.name());
      } else {
        resource.put("name", handler.uri());
      }

      if (handler.title() != null) {
        resource.put("title", handler.title());
      }

      if (handler.description() != null) {
        resource.put("description", handler.description());
      }

      resources.add(resource);
    }

    ListResourcesResult result = new ListResourcesResult().setResources(resources);

    return Future.succeededFuture(JsonResponse.success(request, result.toJson()));
  }

  private Future<JsonResponse> handleReadResource(ServerRequest serverRequest, JsonRequest request) {
    JsonObject params = request.getNamedParams();
    if (params == null || !params.containsKey("uri")) {
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.invalidParams("Missing 'uri' parameter"))
      );
    }

    String uri = params.getString("uri");

    // Try static resources first
    for (StaticResourceHandler handler : staticHandlers) {
      if (handler.uri().equals(uri)) {
        return handler.apply(null)
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

    for (DynamicResourceHandler handler : dynamicHandlers) {
      String template = handler.uri();

      if (matchesTemplate(uri, template)) {
        Map<String, String> uriParams = extractTemplateVariables(uri, template);

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

  private Future<JsonResponse> handleListResourceTemplates(ServerRequest serverRequest, JsonRequest request) {
    List<ResourceTemplate> templates = new ArrayList<>();

    // Convert dynamic handlers to resource templates
    for (DynamicResourceHandler handler : dynamicHandlers) {
      ResourceTemplate template = new ResourceTemplate().setUriTemplate(handler.uri());

      if (handler.name() != null) {
        template.setName(handler.name());
      }

      if (handler.title() != null) {
        template.setTitle(handler.title());
      }

      if (handler.description() != null) {
        template.setDescription(handler.description());
      }

      templates.add(template);
    }

    ListResourceTemplatesResult result = new ListResourceTemplatesResult().setResourceTemplates(templates);

    return Future.succeededFuture(JsonResponse.success(request, result.toJson()));
  }

  /**
   * Utility method to check if a URI matches a URI uri pattern. Template variables are denoted by curly braces, e.g., "resource://{id}/details" or "{type}://resource/data"
   *
   * @param uri the URI to match
   * @param template the URI uri pattern
   * @return true if the URI matches the uri pattern
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

      // Check if this is a complete uri variable (e.g., {id})
      if (templatePart.startsWith("{") && templatePart.endsWith("}")) {
        // Variable segment - matches any non-empty value
        if (uriPart.isEmpty()) {
          return false;
        }
        continue;
      }

      // Check if this segment contains uri variables (e.g., {type}:)
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
   * Matches a URI segment against a uri segment that contains variables. For example, matches "file:" against "{type}:"
   *
   * @param uriSegment the URI segment to match
   * @param templateSegment the uri segment with variables
   * @return true if the URI segment matches the uri
   */
  private boolean matchesSegmentWithVariables(String uriSegment, String templateSegment) {
    int templatePos = 0;
    int uriPos = 0;

    while (templatePos < templateSegment.length()) {
      if (templateSegment.charAt(templatePos) == '{') {
        // Find the closing brace
        int closeBrace = templateSegment.indexOf('}', templatePos);
        if (closeBrace == -1) {
          return false; // Malformed uri
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
   * Extracts uri variables from a URI given a uri pattern. For example, given URI "resource://user/123" and uri "resource://user/{id}", returns {"id": "123"}
   *
   * @param uri the URI to extract variables from
   * @param template the URI uri pattern
   * @return map of variable names to their values
   */
  private java.util.Map<String, String> extractTemplateVariables(String uri, String template) {
    java.util.Map<String, String> variables = new java.util.HashMap<>();

    String[] uriParts = uri.split("/");
    String[] templateParts = template.split("/");

    for (int i = 0; i < templateParts.length && i < uriParts.length; i++) {
      String templatePart = templateParts[i];
      String uriPart = uriParts[i];

      // Check if this is a complete uri variable (e.g., {id})
      if (templatePart.startsWith("{") && templatePart.endsWith("}")) {
        String varName = templatePart.substring(1, templatePart.length() - 1);
        variables.put(varName, uriPart);
        continue;
      }

      // Check if this segment contains uri variables (e.g., {type}:)
      if (templatePart.contains("{") && templatePart.contains("}")) {
        extractSegmentVariables(uriPart, templatePart, variables);
      }
    }

    return variables;
  }

  /**
   * Extracts variables from a URI segment that matches a uri segment with variables. For example, given "file:" and "{type}:", extracts {"type": "file"}
   *
   * @param uriSegment the URI segment
   * @param templateSegment the uri segment with variables
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
          return; // Malformed uri
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

  public void addStaticResource(String uri, Supplier<Future<Resource>> resourceSupplier) {
    addStaticResource(uri, uri, resourceSupplier);
  }

  public void addStaticResource(String uri, String name, Supplier<Future<Resource>> resourceSupplier) {
    addStaticResource(uri, name, null, resourceSupplier);
  }

  public void addStaticResource(String uri, String name, String title, Supplier<Future<Resource>> resourceSupplier) {
    addStaticResource(uri, name, title, null, resourceSupplier);
  }

  public void addStaticResource(String uri, String name, String title, String description, Supplier<Future<Resource>> resourceSupplier) {
    addStaticResource(StaticResourceHandler.create(uri, name, title, description, resourceSupplier));
  }

  public void addStaticResource(StaticResourceHandler handler) {
    staticHandlers.add(handler);
  }

  public void addDynamicResource(String uri, Function<Map<String, String>, Future<Resource>> resourceFunction) {
    addDynamicResource(uri, uri, resourceFunction);
  }

  public void addDynamicResource(String uri, String name, Function<Map<String, String>, Future<Resource>> resourceFunction) {
    addDynamicResource(uri, name, null, resourceFunction);
  }

  public void addDynamicResource(String uri, String name, String title, Function<Map<String, String>, Future<Resource>> resourceFunction) {
    addDynamicResource(uri, name, title, null, resourceFunction);
  }

  public void addDynamicResource(String uri, String name, String title, String description, Function<Map<String, String>, Future<Resource>> resourceFunction) {
    addDynamicResource(uri, name, title, description, resourceFunction, null);
  }

  public void addDynamicResource(String uri, String name, String title, String description, Function<Map<String, String>, Future<Resource>> resourceFunction,
    BiFunction<CompletionArgument, CompletionContext, Future<Completion>> completionFunction) {
    addDynamicResource(DynamicResourceHandler.create(uri, name, title, description, resourceFunction, completionFunction));
  }

  public void addDynamicResource(DynamicResourceHandler handler) {
    dynamicHandlers.add(handler);
  }
}
