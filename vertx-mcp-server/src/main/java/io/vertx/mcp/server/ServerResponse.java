package io.vertx.mcp.server;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.WriteStream;
import io.vertx.mcp.common.rpc.JsonBatch;
import io.vertx.mcp.common.rpc.JsonResponse;

public interface ServerResponse extends WriteStream<JsonObject> {

  void init();

  ContextInternal context();

  Future<Void> end(JsonResponse response);

  Future<Void> end(JsonBatch batch);

  /**
   * End the response with 202 Accepted status.
   * This is used for notifications which don't expect a JSON-RPC response.
   *
   * @return a future that will be completed when the response ends
   */
  Future<Void> endWithAccepted();

  /**
   * Get the session associated with this response.
   * Sessions are used to track SSE connections and streaming state.
   *
   * @return the session, or null if no session exists
   */
  Session session();

  /**
   * Set the session for this response.
   *
   * @param session the session
   */
  void setSession(Session session);

}

