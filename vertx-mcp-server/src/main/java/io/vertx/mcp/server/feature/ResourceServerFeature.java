package io.vertx.mcp.server.feature;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.completion.Completion;
import io.vertx.mcp.common.completion.CompletionArgument;
import io.vertx.mcp.common.completion.CompletionContext;
import io.vertx.mcp.common.notification.ResourceUpdatedNotification;
import io.vertx.mcp.common.resources.Resource;
import io.vertx.mcp.common.resources.ResourceTemplate;
import io.vertx.mcp.common.result.ListResourceTemplatesResult;
import io.vertx.mcp.common.result.ListResourcesResult;
import io.vertx.mcp.common.result.ReadResourceResult;
import io.vertx.mcp.common.rpc.JsonError;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.server.*;
import io.vertx.mcp.server.impl.ServerFeatureBase;
import io.vertx.mcp.server.impl.ServerFeatureStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The ResourceServerFeature class implements the ServerFeatureBase and provides functionality to handle JSON-RPC requests related to resource management. This includes listing
 * available resources, reading resource content, and managing static and dynamic resource handlers.
 *
 * @version 2025-06-18
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/server/resources">Server Features - Resources</a>
 */
public class ResourceServerFeature extends ServerFeatureBase implements CompletionProvider, SubscriptionProvider {

  private final ServerFeatureStorage<StaticResourceHandler> staticHandlers;
  private final ServerFeatureStorage<DynamicResourceHandler> dynamicHandlers;
  private final Map<String, Set<String>> subscriptions = new ConcurrentHashMap<>();
  private Vertx vertx;

  public ResourceServerFeature() {
    this.staticHandlers = new ServerFeatureStorage<>(() -> vertx, "notifications/resources/list_changed");
    this.dynamicHandlers = new ServerFeatureStorage<>(() -> vertx, "notifications/resources/list_changed");
  }

  @Override
  public void init(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public Map<String, BiFunction<ServerRequest, JsonRequest, Future<JsonResponse>>> getHandlers() {
    return Map.of(
      "resources/list", this::handleListResources,
      "resources/read", this::handleReadResource,
      "resources/templates/list", this::handleListResourceTemplates
    );
  }

  @Override
  public Future<Completion> handleCompletion(String refType, String refName, CompletionArgument argument, CompletionContext context) {
    // For completion, refName is the URI template itself (e.g., "resource://user/{id}")
    // Find the handler with matching template
    for (DynamicResourceHandler handler : dynamicHandlers.values()) {
      String handlerTemplate = handler.uri();

      // Direct template match or pattern match for completion
      if (handlerTemplate.equals(refName) || templateMatchesPattern(refName, handlerTemplate)) {
        // Delegate to the handler's completion function
        Future<Completion> completionFuture = handler.completion(argument, context);
        if (completionFuture != null) {
          return completionFuture;
        }
      }
    }

    // Return empty completion if no handler found or no completion function
    return Future.succeededFuture(new Completion()
      .setValues(new ArrayList<>())
      .setTotal(0)
      .setHasMore(false));
  }

  /**
   * Checks if two URI templates could match the same pattern. This compares the structure of templates for completion purposes.
   */
  private boolean templateMatchesPattern(String template1, String template2) {
    // Normalize both templates by replacing variables with a placeholder
    String normalized1 = template1.replaceAll("\\{[^}]+\\}", "{var}");
    String normalized2 = template2.replaceAll("\\{[^}]+\\}", "{var}");
    return normalized1.equals(normalized2);
  }

  @Override
  public Set<String> getCompletionCapabilities() {
    return Set.of("ref/resource");
  }

  @Override
  public Future<Boolean> validateSubscription(String uri) {
    // Check if URI matches any static resource
    for (StaticResourceHandler handler : staticHandlers.values()) {
      if (handler.uri().equals(uri)) {
        return Future.succeededFuture(true);
      }
    }

    // Check if URI matches any dynamic resource template
    for (DynamicResourceHandler handler : dynamicHandlers.values()) {
      if (matchesTemplate(uri, handler.uri())) {
        return Future.succeededFuture(true);
      }
    }

    return Future.succeededFuture(false);
  }

  @Override
  public void subscribe(String sessionId, String uri) {
    subscriptions.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet()).add(uri);
  }

  @Override
  public void unsubscribe(String sessionId, String uri) {
    Set<String> sessionSubs = subscriptions.get(sessionId);
    if (sessionSubs != null) {
      sessionSubs.remove(uri);
      if (sessionSubs.isEmpty()) {
        subscriptions.remove(sessionId);
      }
    }
  }

  /**
   * Notifies all subscribed sessions that a resource has been updated. This sends a ResourceUpdatedNotification to each session that is subscribed to the given URI.
   *
   * @param vertx the Vertx instance to use for event bus communication
   * @param uri the URI of the resource that was updated
   */
  public void notifyResourceUpdated(Vertx vertx, String uri) {
    ResourceUpdatedNotification notification = new ResourceUpdatedNotification().setUri(uri);

    // Find all sessions subscribed to this URI and notify them
    for (Map.Entry<String, Set<String>> entry : subscriptions.entrySet()) {
      String sessionId = entry.getKey();
      Set<String> subscribedUris = entry.getValue();

      // Check if this session is subscribed to the URI (exact match or template match)
      boolean isSubscribed = subscribedUris.contains(uri);
      if (!isSubscribed) {
        // Check for template matches
        for (String subscribedUri : subscribedUris) {
          if (matchesTemplate(uri, subscribedUri)) {
            isSubscribed = true;
            break;
          }
        }
      }

      if (isSubscribed) {
        DeliveryOptions options = new DeliveryOptions().addHeader("Mcp-Session-Id", sessionId);
        vertx.eventBus().send(SessionManager.NOTIFICATION_ADDRESS, notification.toNotification().toJson(), options);
      }
    }
  }

  private Future<JsonResponse> handleListResources(ServerRequest serverRequest, JsonRequest request) {
    JsonArray resources = new JsonArray();

    for (StaticResourceHandler handler : staticHandlers.values()) {
      resources.add(handler.toFeature().toJson());
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
    for (StaticResourceHandler handler : staticHandlers.values()) {
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

    for (DynamicResourceHandler handler : dynamicHandlers.values()) {
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

    for (DynamicResourceHandler handler : dynamicHandlers.values()) {
      templates.add(handler.toFeature());
    }

    ListResourceTemplatesResult result = new ListResourceTemplatesResult().setResourceTemplates(templates);

    return Future.succeededFuture(JsonResponse.success(request, result.toJson()));
  }

  /**
   * Converts a URI template to a regex pattern for matching. Template variables like {id} are converted to named capture groups.
   *
   * @param template the URI template
   * @return the compiled regex pattern
   */
  private Pattern templateToPattern(String template) {
    // Escape regex special characters except { and }
    String regex = template
      .replace(".", "\\.")
      .replace("?", "\\?")
      .replace("+", "\\+")
      .replace("*", "\\*")
      .replace("[", "\\[")
      .replace("]", "\\]")
      .replace("(", "\\(")
      .replace(")", "\\)")
      .replace("^", "\\^")
      .replace("$", "\\$")
      .replace("|", "\\|");

    // Replace {varname} with named capture groups
    regex = regex.replaceAll("\\{([^}]+)\\}", "(?<$1>[^/]+)");

    return Pattern.compile("^" + regex + "$");
  }

  /**
   * Utility method to check if a URI matches a URI template pattern. Template variables are denoted by curly braces, e.g., "resource://user/{id}"
   *
   * @param uri the URI to match
   * @param template the URI template pattern
   * @return true if the URI matches the template pattern
   */
  private boolean matchesTemplate(String uri, String template) {
    try {
      Pattern pattern = templateToPattern(template);
      return pattern.matcher(uri).matches();
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Extracts template variables from a URI given a template pattern. For example, given URI "resource://user/123" and template "resource://user/{id}", returns {"id": "123"}
   *
   * @param uri the URI to extract variables from
   * @param template the URI template pattern
   * @return map of variable names to their values
   */
  private Map<String, String> extractTemplateVariables(String uri, String template) {
    Map<String, String> variables = new java.util.HashMap<>();

    try {
      Pattern pattern = templateToPattern(template);
      Matcher matcher = pattern.matcher(uri);

      if (matcher.matches()) {
        // Extract all named groups
        Pattern varPattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher varMatcher = varPattern.matcher(template);

        while (varMatcher.find()) {
          String varName = varMatcher.group(1);
          try {
            String value = matcher.group(varName);
            if (value != null) {
              variables.put(varName, value);
            }
          } catch (IllegalArgumentException e) {
            // Group not found, skip
          }
        }
      }
    } catch (Exception e) {
      // Return empty map on error
    }

    return variables;
  }

  /**
   * Registers a static resource to be served under the specified URI.
   *
   * @param uri the URI at which the resource will be served
   * @param resourceSupplier a supplier providing a Future that resolves to the resource to be served
   */
  public void addStaticResource(String uri, Supplier<Future<Resource>> resourceSupplier) {
    addStaticResource(uri, uri, resourceSupplier);
  }

  /**
   * Registers a static resource with the specified URI, name, and supplier.
   *
   * @param uri the unique identifier for the resource
   * @param name the name of the resource
   * @param resourceSupplier a supplier of the resource, providing a future that resolves to the resource
   */
  public void addStaticResource(String uri, String name, Supplier<Future<Resource>> resourceSupplier) {
    addStaticResource(uri, name, null, resourceSupplier);
  }

  /**
   * Adds a static resource to the resource server. This method registers a resource with the specified URI, name, and title, using the provided resource supplier to supply the
   * resource content dynamically.
   *
   * @param uri the unique URI of the resource
   * @param name the name of the resource
   * @param title the title of the resource
   * @param resourceSupplier a supplier that provides a {@link Future} containing the resource
   */
  public void addStaticResource(String uri, String name, String title, Supplier<Future<Resource>> resourceSupplier) {
    addStaticResource(uri, name, title, null, resourceSupplier);
  }

  /**
   * Adds a static resource to the resource server.
   *
   * @param uri the unique URI associated with the resource
   * @param name the name of the resource
   * @param title the title of the resource
   * @param description a brief description of the resource
   * @param resourceSupplier a supplier that provides a future containing the resource instance
   */
  public void addStaticResource(String uri, String name, String title, String description, Supplier<Future<Resource>> resourceSupplier) {
    addStaticResource(StaticResourceHandler.create(uri, name, title, description, resourceSupplier));
  }

  /**
   * Adds a static resource handler to the resource server.
   *
   * @param handler the static resource handler to be added. The handler provides a resource at a fixed URI that does not require any parameters.
   */
  public void addStaticResource(StaticResourceHandler handler) {
    staticHandlers.put(handler.uri(), handler);
  }

  /**
   * Adds a dynamic resource to the resource server. The resource is identified by the given URI and can be dynamically generated based on the provided function, which evaluates
   * template variables in the URI.
   *
   * @param uri the unique identifier of the resource, potentially containing template variables
   * @param resourceFunction a function that accepts a map of template variables extracted from the URI and returns a Future containing the dynamically generated Resource
   */
  public void addDynamicResource(String uri, Function<Map<String, String>, Future<Resource>> resourceFunction) {
    addDynamicResource(uri, uri, resourceFunction);
  }

  /**
   * Adds a dynamic resource to the resource server with the specified URI, name, and resource provider function. The added resource is resolved dynamically at runtime based on the
   * provided function.
   *
   * @param uri the URI of the dynamic resource
   * @param name the name of the dynamic resource
   * @param resourceFunction a function that takes a map of template variables as input and returns a Future containing the resolved resource
   */
  public void addDynamicResource(String uri, String name, Function<Map<String, String>, Future<Resource>> resourceFunction) {
    addDynamicResource(uri, name, null, resourceFunction);
  }

  /**
   * Registers a dynamic resource with the specified URI, name, and title.
   *
   * @param uri the URI of the resource
   * @param name the name of the resource
   * @param title the title of the resource
   * @param resourceFunction a function that generates the resource dynamically based on the provided parameters
   */
  public void addDynamicResource(String uri, String name, String title, Function<Map<String, String>, Future<Resource>> resourceFunction) {
    addDynamicResource(uri, name, title, null, resourceFunction);
  }

  /**
   * Adds a dynamic resource to the resource server. A dynamic resource is resolved at runtime by invoking the provided resource function.
   *
   * @param uri the URI of the dynamic resource
   * @param name the name of the dynamic resource
   * @param title the title of the dynamic resource
   * @param description the description of the dynamic resource
   * @param resourceFunction a function that takes a map of template variables and returns a future resolving to the resource
   */
  public void addDynamicResource(String uri, String name, String title, String description, Function<Map<String, String>, Future<Resource>> resourceFunction) {
    addDynamicResource(uri, name, title, description, resourceFunction, null);
  }

  /**
   * Adds a dynamic resource to the resource server with custom handling for resource creation and completion functionality.
   *
   * @param uri the unique URI identifying the resource
   * @param name the name of the resource
   * @param title the title of the resource
   * @param description a brief description of the resource
   * @param resourceFunction a function that generates the resource content dynamically given a map of URI template variables
   * @param completionFunction a function handling completion tasks associated with the resource, where the completion data is based on the provided arguments and context
   */
  public void addDynamicResource(String uri, String name, String title, String description, Function<Map<String, String>, Future<Resource>> resourceFunction,
    BiFunction<CompletionArgument, CompletionContext, Future<Completion>> completionFunction) {
    addDynamicResource(DynamicResourceHandler.create(uri, name, title, description, resourceFunction, completionFunction));
  }

  /**
   * Registers a dynamic resource handler that provides resources based on URI patterns.
   *
   * @param handler the dynamic resource handler to be added. The handler contains the URI pattern and logic for producing resources dynamically based on extracted URI variables.
   */
  public void addDynamicResource(DynamicResourceHandler handler) {
    dynamicHandlers.put(handler.uri(), handler);
  }

  /**
   * Retrieves a list of static resource handlers managed by this feature.
   *
   * @return a list of {@link StaticResourceHandler} instances representing the static resources.
   */
  public List<StaticResourceHandler> staticResources() {
    return new ArrayList<>(this.staticHandlers.values());
  }

  /**
   * Retrieves a list of dynamic resource handlers managed by this feature.
   *
   * @return a list of {@link DynamicResourceHandler} instances representing the dynamic resources.
   */
  public List<DynamicResourceHandler> dynamicResources() {
    return new ArrayList<>(this.dynamicHandlers.values());
  }
}
