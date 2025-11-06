package io.vertx.mcp.server;

import io.vertx.core.Future;
import io.vertx.mcp.common.rpc.JsonResponse;

/**
 * Represents a session between the client and server.
 * Sessions are used to manage SSE (Server-Sent Events) connections and streaming state.
 */
public interface Session {

  /**
   * Get the unique session ID.
   *
   * @return the session ID
   */
  String id();

  /**
   * Check if this session is using SSE (Server-Sent Events).
   *
   * @return true if SSE is enabled for this session
   */
  boolean isSse();

  /**
   * Enable SSE mode for this session.
   * Once enabled, the server can send multiple responses to the client.
   */
  void enableSse();

  /**
   * Check if the session is still active.
   *
   * @return true if the session is active
   */
  boolean isActive();

  /**
   * Send a response through this session.
   * For SSE sessions, this sends an event. For regular sessions, this is the final response.
   *
   * @param response the JSON-RPC response to send
   * @return a future that completes when the response has been sent
   */
  Future<Void> send(JsonResponse response);

  /**
   * Close the session.
   * For SSE connections, this closes the event stream.
   *
   * @return a future that completes when the session is closed
   */
  Future<Void> close();

}
