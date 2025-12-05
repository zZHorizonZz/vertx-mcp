package io.vertx.mcp.client;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Closeable;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.capabilities.ServerCapabilities;
import io.vertx.mcp.common.notification.Notification;
import io.vertx.mcp.common.request.Request;
import io.vertx.mcp.common.rpc.JsonNotification;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;

/**
 * Represents a session between the client and server. Sessions are used to manage SSE (Server-Sent Events) connections and streaming state.
 */
@VertxGen
public interface ClientSession extends Handler<JsonObject>, Closeable {

  String MCP_SESSION_CONTEXT_KEY = "mcp.client.session";

  /**
   * Retrieve the current session from the Vert.x context.
   *
   * @param context the Vert.x context
   * @return the session, or null if no session is stored in the context
   */
  static ClientSession fromContext(Context context) {
    return context.get(MCP_SESSION_CONTEXT_KEY);
  }

  /**
   * Get the unique session ID.
   *
   * @return the session ID
   */
  String id();

  /**
   * Gets the server capabilities negotiated during initialization.
   *
   * @return server capabilities
   */
  ServerCapabilities serverCapabilities();

  /**
   * Sends a sendRequest to the server.
   *
   * @param request the sendRequest to send
   * @return a future that completes with the result
   */
  Future<JsonResponse> sendRequest(Request request);

  Future<JsonResponse> sendRequest(JsonRequest request);

  /**
   * Sends a notification to the server. This method is typically used to deliver an asynchronous notification without expecting any response from the server.
   *
   * @param notification the notification to be sent, containing the method name and associated metadata
   * @return a future that completes when the notification has been sent successfully
   */
  Future<Void> sendNotification(Notification notification);

  Future<Void> sendNotification(JsonNotification notification);

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
}
