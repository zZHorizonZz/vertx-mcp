package io.vertx.mcp.server;

import io.vertx.core.Future;

/**
 * Interface for server features that support resource subscriptions. Features implementing this interface can validate subscription requests and notify when resources change.
 *
 * @version 2025-06-18
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/server/resources#subscriptions">Server Features - Resources - Subscriptions</a>
 */
public interface SubscriptionProvider {

  /**
   * Validates if a resource URI can be subscribed to.
   *
   * @param uri the resource URI to subscribe to
   * @return a Future that succeeds with true if the subscription is valid, false otherwise
   */
  Future<Boolean> validateSubscription(String uri);

  /**
   * Called when a subscription is added.
   *
   * @param sessionId the session ID
   * @param uri the resource URI
   */
  void subscribe(String sessionId, String uri);

  /**
   * Called when a subscription is removed.
   *
   * @param sessionId the session ID
   * @param uri the resource URI
   */
  void unsubscribe(String sessionId, String uri);
}
