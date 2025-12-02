package io.vertx.mcp.client;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import io.vertx.mcp.common.rpc.JsonResponse;

/**
 * Represents the client response in a JSON-RPC framework. This interface provides mechanisms for accessing and managing JSON-RPC responses and notifications received from the
 * server.
 */
@VertxGen
public interface ClientResponse extends ReadStream<JsonObject> {

  /**
   * Initializes the client response with the given session and client request. This method associates the response with a specific session and request, allowing state and
   * connections to be managed.
   *
   * @param session the session to associate with the client response
   * @param request the client request associated with this response
   */
  void init(ClientSession session, ClientRequest request);

  /**
   * Retrieves the identifier associated with the current client response request.
   *
   * @return the request identifier, typically associated with a JSON-RPC request
   */
  Object requestId();

  /**
   * Retrieve the current session associated with the client response.
   *
   * @return the current {@code ClientSession} object
   */
  ClientSession session();

  /**
   * Retrieves the internal Vert.x context associated with the current client response.
   *
   * @return the internal context of the client response
   */
  @GenIgnore
  ContextInternal context();

  /**
   * Retrieves the JSON-RPC response associated with this client response.
   *
   * @return the associated {@link JsonResponse} object
   */
  JsonResponse getJsonResponse();

  /**
   * Retrieves the client request associated with this response.
   *
   * @return the associated {@link ClientRequest} object
   */
  ClientRequest request();
}
