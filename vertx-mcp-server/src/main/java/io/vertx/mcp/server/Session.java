package io.vertx.mcp.server;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.Future;
import io.vertx.mcp.common.rpc.JsonResponse;

/**
 * Represents a session between the client and server. Sessions are used to manage SSE (Server-Sent Events) connections and streaming state.
 */
@DataObject
public interface Session {

  /**
   * Get the unique session ID.
   *
   * @return the session ID
   */
  String id();

  /**
   * Checks if the session is currently in a streaming state.
   *
   * @return true if the session is streaming, false otherwise
   */
  boolean isStreaming();

  /**
   * Check if the session is still active.
   *
   * @return true if the session is active
   */
  boolean isActive();

  /**
   * Send a response through this session. For streaming sessions, this sends an event. For regular sessions, this is the final response.
   *
   * @param response the JSON-RPC response to send
   * @return a future that completes when the response has been sent
   */
  Future<Void> send(JsonResponse response);
}
