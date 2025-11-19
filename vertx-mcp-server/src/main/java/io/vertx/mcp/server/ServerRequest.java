package io.vertx.mcp.server;

import io.vertx.codegen.annotations.CacheReturn;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.internal.ContextInternal;
import io.vertx.mcp.common.rpc.JsonRequest;

/**
 * The {@code ServerRequest} interface defines the structure and behavior of a server request in the JSON-RPC framework. It provides methods to manage and access the request's
 * context, session, response, and other associated information required to handle client-server interactions.
 */
@VertxGen
public interface ServerRequest {

  /**
   * Initializes the server request with the specified session and server response. This method associates the request with a session and response object, enabling subsequent
   * operations to link client-server communication through the session.
   *
   * @param session the session representing the client-server connection context
   * @param response the server response object to manage outgoing responses
   */
  void init(ServerSession session, ServerResponse response);

  /**
   * Retrieves the path associated with this server request. The path typically represents the requested resource or endpoint.
   *
   * @return the path of the server request
   */
  @CacheReturn
  String path();

  /**
   * Provides access to the internal context associated with the server request.
   *
   * @return the internal context linked to this server request
   */
  @GenIgnore
  ContextInternal context();

  /**
   * Provides access to the server response associated with this request. The server response allows writing and managing JSON-RPC responses or notifications.
   *
   * @return the {@code ServerResponse} instance associated with this request
   */
  @CacheReturn
  ServerResponse response();

  /**
   * Retrieves the JSON-RPC request associated with the current server request.
   *
   * @return the associated {@link JsonRequest} object
   */
  @CacheReturn
  JsonRequest getJsonRequest();

  /**
   * Retrieves the current session associated with the server request.
   *
   * @return the current session, or null if no session is associated with the request
   */
  ServerSession session();
}
