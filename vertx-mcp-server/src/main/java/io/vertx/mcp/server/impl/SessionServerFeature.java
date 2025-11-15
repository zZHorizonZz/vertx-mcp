package io.vertx.mcp.server.impl;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.rpc.JsonError;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.server.ServerFeature;
import io.vertx.mcp.server.ServerOptions;
import io.vertx.mcp.server.ServerRequest;
import io.vertx.mcp.server.Session;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles session management for SSE connections, including subscribe/unsubscribe operations.
 * This feature enables clients to subscribe to server notifications and events.
 */
public class SessionServerFeature implements ServerFeature {

  private final ServerOptions options;
  // Map of session ID to subscribed resource URIs
  private final Map<String, Set<String>> subscriptions = new ConcurrentHashMap<>();
  // Track active session count
  private final AtomicInteger sessionCount = new AtomicInteger(0);

  public SessionServerFeature(ServerOptions options) {
    this.options = options;
  }

  @Override
  public void handle(ServerRequest serverRequest) {
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
      case "resources/subscribe":
        responseFuture = handleSubscribe(serverRequest, request);
        break;
      case "resources/unsubscribe":
        responseFuture = handleUnsubscribe(serverRequest, request);
        break;
      case "notifications/initialized":
        responseFuture = handleInitializeNotifications(serverRequest, request);
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

  private Future<JsonResponse> handleSubscribe(ServerRequest serverRequest, JsonRequest request) {
    // Check if sessions are enabled
    if (!options.getSessionsEnabled()) {
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.methodNotAllowed())
      );
    }

    Session session = serverRequest.session();

    if (session == null) {
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.invalidRequest("No session available"))
      );
    }

    JsonObject params = request.getNamedParams();
    if (params == null || !params.containsKey("uri")) {
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.invalidParams("Missing 'uri' parameter"))
      );
    }

    String uri = params.getString("uri");

    subscriptions.computeIfAbsent(session.id(), k -> ConcurrentHashMap.newKeySet()).add(uri);

    JsonObject result = new JsonObject().put("subscribed", true);
    return Future.succeededFuture(JsonResponse.success(request, result));
  }

  private Future<JsonResponse> handleUnsubscribe(ServerRequest serverRequest, JsonRequest request) {
    // Check if sessions are enabled
    if (!options.getSessionsEnabled()) {
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.methodNotAllowed())
      );
    }

    Session session = serverRequest.session();

    if (session == null) {
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.invalidRequest("No session available"))
      );
    }

    JsonObject params = request.getNamedParams();
    if (params == null || !params.containsKey("uri")) {
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.invalidParams("Missing 'uri' parameter"))
      );
    }

    String uri = params.getString("uri");

    Set<String> sessionSubs = subscriptions.get(session.id());
    if (sessionSubs != null) {
      sessionSubs.remove(uri);
      if (sessionSubs.isEmpty()) {
        subscriptions.remove(session.id());
      }
    }

    JsonObject result = new JsonObject().put("unsubscribed", true);
    return Future.succeededFuture(JsonResponse.success(request, result));
  }

  private Future<JsonResponse> handleInitializeNotifications(ServerRequest serverRequest, JsonRequest request) {
    // Check if sessions and streaming are enabled
    if (!options.getSessionsEnabled()) {
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.methodNotAllowed())
      );
    }

    if (!options.getStreamingEnabled()) {
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.invalidRequest("Streaming is not enabled on this server"))
      );
    }

    Session session = serverRequest.session();

    if (session == null) {
      // Create a new session for this request
      // The transport layer should handle this, but we can enable SSE here
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.internalError("Session management not available in transport"))
      );
    }

    // Check session limit
    if (sessionCount.get() >= options.getMaxSessions()) {
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.serverError(-32001, "Maximum session limit reached"))
      );
    }

    sessionCount.incrementAndGet();

    JsonObject result = new JsonObject()
      .put("sessionId", session.id())
      .put("sseEnabled", true);

    return Future.succeededFuture(JsonResponse.success(request, result));
  }

  /**
   * Check if a session is subscribed to a specific URI.
   *
   * @param sessionId the session ID
   * @param uri the resource URI
   * @return true if subscribed
   */
  public boolean isSubscribed(String sessionId, String uri) {
    Set<String> sessionSubs = subscriptions.get(sessionId);
    return sessionSubs != null && sessionSubs.contains(uri);
  }

  /**
   * Get all subscriptions for a session.
   *
   * @param sessionId the session ID
   * @return set of subscribed URIs
   */
  public Set<String> getSubscriptions(String sessionId) {
    return subscriptions.getOrDefault(sessionId, Set.of());
  }

  /**
   * Remove all subscriptions for a session (typically when session closes).
   *
   * @param sessionId the session ID
   */
  public void removeSession(String sessionId) {
    subscriptions.remove(sessionId);
    sessionCount.decrementAndGet();
  }

  /**
   * Gets the current number of active sessions.
   *
   * @return the session count
   */
  public int getSessionCount() {
    return sessionCount.get();
  }

  @Override
  public Set<String> getCapabilities() {
    return Set.of("resources/subscribe", "resources/unsubscribe", "notifications/initialized");
  }
}
