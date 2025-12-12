package io.vertx.mcp.server.feature;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.mcp.common.request.SubscribeRequest;
import io.vertx.mcp.common.request.UnsubscribeRequest;
import io.vertx.mcp.common.result.EmptyResult;
import io.vertx.mcp.common.rpc.JsonError;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.server.*;
import io.vertx.mcp.server.impl.ServerFeatureBase;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

/**
 * The SessionServerFeature class implements the ServerFeatureBase and provides functionality to handle JSON-RPC requests related to session management. This includes handling
 * subscriptions, unsubscriptions, and session notifications for SSE connections.
 *
 * @version 2025-06-18
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/basic/transports#session-management">Server Features - Sessions</a>
 */
public class SessionServerFeature extends ServerFeatureBase {

  private final AtomicInteger sessionCount = new AtomicInteger(0);
  private final Map<String, Set<String>> subscriptions = new ConcurrentHashMap<>();

  @Override
  public Map<String, BiFunction<ServerRequest, JsonRequest, Future<JsonResponse>>> getHandlers() {
    return Map.of(
      "resources/subscribe", this::handleSubscribe,
      "resources/unsubscribe", this::handleUnsubscribe,
      "notifications/initialized", this::handleInitializeNotifications
    );
  }

  private Future<JsonResponse> handleSubscribe(ServerRequest serverRequest, JsonRequest request) {
    if (!getServer().getOptions().getStreamingEnabled()) {
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.methodNotAllowed())
      );
    }

    ServerSession session = serverRequest.session();

    if (session == null) {
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.invalidRequest("No session available"))
      );
    }

    SubscribeRequest subscribe = new SubscribeRequest(request.getNamedParams());
    if (subscribe.getUri() == null || subscribe.getUri().isEmpty()) {
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.invalidParams("Missing 'uri' parameter"))
      );
    }

    String uri = subscribe.getUri();
    SubscriptionProvider provider = findSubscriptionProvider();

    if (provider == null) {
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.internalError("No subscription provider available"))
      );
    }

    return provider.validateSubscription(uri).compose(valid -> {
      if (!valid) {
        return Future.succeededFuture(
          JsonResponse.error(request, JsonError.invalidParams("Resource not found: " + uri))
        );
      }

      subscriptions.computeIfAbsent(session.id(), k -> ConcurrentHashMap.newKeySet()).add(uri);

      provider.subscribe(session.id(), uri);

      return Future.succeededFuture(new EmptyResult().toResponse(request));
    });
  }

  private SubscriptionProvider findSubscriptionProvider() {
    for (ServerFeature feature : getServer().features()) {
      if (feature instanceof SubscriptionProvider) {
        return (SubscriptionProvider) feature;
      }
    }
    return null;
  }

  private Future<JsonResponse> handleUnsubscribe(ServerRequest serverRequest, JsonRequest request) {
    if (!getServer().getOptions().getStreamingEnabled()) {
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.methodNotAllowed())
      );
    }

    ServerSession session = serverRequest.session();

    if (session == null) {
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.invalidRequest("No session available"))
      );
    }

    UnsubscribeRequest unsubscribe = new UnsubscribeRequest(request.getNamedParams());
    if (unsubscribe.getUri() == null || unsubscribe.getUri().isEmpty()) {
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.invalidParams("Missing 'uri' parameter"))
      );
    }

    String uri = unsubscribe.getUri();

    Set<String> sessionSubs = subscriptions.get(session.id());
    if (sessionSubs != null) {
      sessionSubs.remove(uri);
      if (sessionSubs.isEmpty()) {
        subscriptions.remove(session.id());
      }

      SubscriptionProvider provider = findSubscriptionProvider();
      if (provider != null) {
        provider.unsubscribe(session.id(), uri);
      }
    }

    return Future.succeededFuture(new EmptyResult().toResponse(request));
  }

  private Future<JsonResponse> handleInitializeNotifications(ServerRequest serverRequest, JsonRequest request) {
    if (!getServer().getOptions().getStreamingEnabled()) {
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.invalidRequest("Streaming is not enabled on this server"))
      );
    }

    ServerSession session = serverRequest.session();

    if (session == null) {
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.internalError("ServerSession management not available in transport"))
      );
    }

    if (sessionCount.get() >= getServer().getOptions().getMaxSessions()) {
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.serverError(-32001, "Maximum session limit reached"))
      );
    }

    sessionCount.incrementAndGet();

    return Future.succeededFuture(new EmptyResult().toResponse(request));
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
}
