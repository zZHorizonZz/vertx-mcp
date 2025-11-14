package io.vertx.mcp.server;

import io.vertx.core.internal.ContextInternal;
import io.vertx.mcp.common.rpc.JsonRequest;

public interface ServerRequest {

  void init(Session session, ServerResponse response);

  /**
   * Get the request path for routing. This returns the JSON-RPC method field from the parsed request.
   *
   * @return the method/path, or null if request not yet parsed
   */
  String path();

  ContextInternal context();

  ServerResponse response();

  /**
   * Get the parsed JSON-RPC request. This is only available after the request has been fully read and parsed.
   *
   * @return the JSON-RPC request, or null if not yet parsed
   */
  JsonRequest getJsonRequest();

  /**
   * Get the session associated with this request. Sessions are used to track SSE connections and streaming state.
   *
   * @return the session, or null if no session exists
   */
  Session session();
}
