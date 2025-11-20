package io.vertx.mcp.server;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.WriteStream;
import io.vertx.mcp.common.rpc.JsonResponse;

/**
 * Represents the server response in a JSON-RPC framework. This interface extends {@link WriteStream} and provides mechanisms for writing and managing JSON-RPC responses and
 * notifications.
 */
@VertxGen
public interface ServerResponse extends WriteStream<JsonObject> {

  /**
   * Initializes the server response with the given session. This method associates the response with a specific session, allowing streaming state and connections to be managed.
   *
   * @param session the session to associate with the server response
   */
  void init(ServerSession session);

  /**
   * Retrieves the identifier associated with the current server response request.
   *
   * @return the request identifier, typically associated with a JSON-RPC request
   */
  Object requestId();

  /**
   * Retrieve the current session associated with the server response.
   *
   * @return the current {@code ServerSession} object
   */
  ServerSession session();

  /**
   * Retrieves the internal Vert.x context associated with the current server response.
   *
   * @return the internal context of the server response
   */
  @GenIgnore
  ContextInternal context();

  /**
   * Writes a JSON-RPC response by converting it into its JSON representation and sending it.
   *
   * @param data the JSON-RPC response to write
   * @return a future that completes when the write operation has finished
   */
  default Future<Void> write(JsonResponse data) {
    return write(data.toJson());
  }

  /**
   * Ends the response by sending the JSON-RPC response content to the client. This method converts the given {@link JsonResponse} object to a {@link JsonObject} and delegates to
   * the overloaded {@code end} method.
   *
   * @param response the {@link JsonResponse} object containing the data to be sent as the final response
   * @return a {@link Future} that completes when the response has been sent
   */
  default Future<Void> end(JsonResponse response) {
    return end(response.toJson());
  }
}

