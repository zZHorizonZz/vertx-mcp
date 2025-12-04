package io.vertx.mcp.client;

import io.vertx.codegen.annotations.CacheReturn;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.mcp.common.rpc.JsonRequest;

/**
 * The {@code ClientRequest} interface defines the structure and behavior of a client sendRequest in the JSON-RPC framework. It provides methods to manage and access the sendRequest's
 * context, session, and other associated information required to handle client-server interactions.
 */
@VertxGen
public interface ClientRequest extends MessageWriteStream {

  /**
   * Retrieves the path associated with this client sendRequest. The path typically represents the requested resource or endpoint.
   *
   * @return the path of the client sendRequest
   */
  @CacheReturn
  String path();

  /**
   * Retrieves the current session associated with the client sendRequest.
   *
   * @return the current session, or null if no session is associated with the sendRequest
   */
  @Nullable
  @CacheReturn
  ClientSession session();

  /**
   * Sends the JSON-RPC sendRequest to the server.
   *
   * @return a future that completes when the sendRequest has been sent
   */
  Future<Void> send(JsonRequest request);

  Future<Void> end(JsonRequest request);

  /**
   * Gets the response future. The response future is only available after calling send().
   *
   * @return the response future, or null if send() has not been called yet
   */
  Future<ClientResponse> response();
}
