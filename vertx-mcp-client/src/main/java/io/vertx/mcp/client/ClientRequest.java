package io.vertx.mcp.client;

import io.vertx.codegen.annotations.CacheReturn;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.internal.ContextInternal;
import io.vertx.mcp.common.rpc.JsonRequest;

/**
 * The {@code ClientRequest} interface defines the structure and behavior of a client request in the JSON-RPC framework. It provides methods to manage and access the request's
 * context, session, and other associated information required to handle client-server interactions.
 */
@VertxGen
public interface ClientRequest {

  /**
   * Retrieves the path associated with this client request. The path typically represents the requested resource or endpoint.
   *
   * @return the path of the client request
   */
  @CacheReturn
  String path();

  /**
   * Provides access to the internal context associated with the client request.
   *
   * @return the internal context linked to this client request
   */
  @GenIgnore
  ContextInternal context();

  /**
   * Provides access to the JSON-RPC request associated with this client request.
   *
   * @return the {@code JsonRequest} instance associated with this request
   */
  @CacheReturn
  JsonRequest getJsonRequest();

  ClientRequest setJsonRequest(JsonRequest jsonRequest);

  /**
   * Sends the JSON-RPC request to the server.
   *
   * @return a future that completes when the request has been sent
   */
  Future<Void> send();

  /**
   * Sends the request and returns the response future.
   *
   * @return a future that completes with the client response
   */
  Future<ClientResponse> sendRequest();

  /**
   * Gets the response future. The response future is only available after calling send().
   *
   * @return the response future, or null if send() has not been called yet
   */
  Future<ClientResponse> response();

  /**
   * Retrieves the current session associated with the client request.
   *
   * @return the current session, or null if no session is associated with the request
   */
  ClientSession session();

  /**
   * Retrieves the request identifier associated with the current client request.
   *
   * @return the request identifier, typically associated with a JSON-RPC request
   */
  Object requestId();
}
