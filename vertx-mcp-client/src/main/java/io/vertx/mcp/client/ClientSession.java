package io.vertx.mcp.client;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Closeable;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.capabilities.ServerCapabilities;
import io.vertx.mcp.common.request.Request;
import io.vertx.mcp.common.result.Result;

/**
 * Represents a session between the client and server. Sessions are used to manage SSE (Server-Sent Events) connections and streaming state.
 */
@VertxGen
public interface ClientSession extends Closeable {

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
  Future<JsonObject> sendRequest(Request request);

  /**
   * Sends the result of a request to the server. This method is typically used in scenarios involving Server-Sent Events (SSE) connections or streaming where a result needs to be
   * sent in response to a specific request.
   *
   * @param request the request object that originated the need to send the result
   * @param result the result object containing the response data to be sent
   * @return a future that completes once the result is successfully sent
   */
  Future<Void> sendResult(Request request, Result result);

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
