package io.vertx.mcp.server;

import io.vertx.core.Future;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.WriteStream;
import io.vertx.mcp.common.rpc.JsonBatch;
import io.vertx.mcp.common.rpc.JsonResponse;

/**
 * Represents the server response in a JSON-RPC framework. This interface extends {@link WriteStream} and provides mechanisms for writing and managing JSON-RPC responses and
 * notifications.
 */
public interface ServerResponse extends WriteStream<JsonObject> {

  /**
   * Initializes the server response with the given session. This method associates the response with a specific session, allowing streaming state and connections to be managed.
   *
   * @param session the session to associate with the server response
   */
  void init(Session session);

  /**
   * Retrieves the internal Vert.x context associated with the current server response.
   *
   * @return the internal context of the server response
   */
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

  /**
   * Ends the response after processing a JSON-RPC batch request or response.
   *
   * @param batch the JSON-RPC batch to be processed and sent in the response
   * @return a Future representing the completion of the response ending operation
   */
  Future<Void> end(JsonBatch batch);

  /**
   * Ends the notification process associated with the server response. This method is used to signal the end of a server-sent notification operation.
   *
   * @return a future that completes when the notification process has successfully ended
   */
  Future<Void> endNotification();

  /**
   * Retrieve the current session associated with the server response.
   *
   * @return the current {@code Session} object
   */
  Session session();
}

