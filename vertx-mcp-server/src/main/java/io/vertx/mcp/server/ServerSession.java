package io.vertx.mcp.server;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Closeable;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.capabilities.ClientCapabilities;
import io.vertx.mcp.common.notification.Notification;
import io.vertx.mcp.common.request.Request;

/**
 * Represents a session between the client and server. Sessions are used to manage SSE (Server-Sent Events) connections and streaming state.
 */
@VertxGen
public interface ServerSession extends Closeable {

  String MCP_SESSION_CONTEXT_KEY = "mcp.session";

  /**
   * Retrieve the current session from the Vert.x context.
   *
   * @param context the Vert.x context
   * @return the session, or null if no session is stored in the context
   */
  static ServerSession fromContext(Context context) {
    return context.get(MCP_SESSION_CONTEXT_KEY);
  }

  /**
   * Get the unique session ID.
   *
   * @return the session ID
   */
  String id();

  /**
   * Gets the client capabilities negotiated during initialization.
   *
   * @return client capabilities
   */
  ClientCapabilities clientCapabilities();

  /**
   * Sends a request to the client.
   *
   * @param request the request to send
   * @return a future that completes with the result
   */
  Future<JsonObject> sendRequest(Request request);

  /**
   * Sends a notification to the client.
   *
   * @param notification the notification to send
   * @return a future that completes when the notification is sent
   */
  Future<Void> sendNotification(Notification notification);

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
   * Gets the minimum log level for this session.
   *
   * @return the minimum log level, or null if not set
   */
  String minLogLevel();

  /**
   * Sets the minimum log level for this session.
   *
   * @param level the minimum log level
   * @return this session for fluent API
   */
  ServerSession setMinLogLevel(String level);
}
