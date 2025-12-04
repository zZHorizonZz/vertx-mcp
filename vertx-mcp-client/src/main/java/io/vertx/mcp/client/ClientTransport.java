package io.vertx.mcp.client;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.mcp.common.capabilities.ClientCapabilities;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.common.transport.Session;

/**
 * The {@code ClientTransport} interface represents the transport layer used to communicate with the MCP server. Implementations of this interface handle connection initialization,
 * sending and receiving JSON-RPC requests, and processing server responses.
 */
@VertxGen
public interface ClientTransport {
  /**
   * Connects to the MCP server and performs the initialization handshake.
   *
   * @param capabilities the client capabilities
   * @return a future that completes with the initialized session
   */
  Future<ClientSession> subscribe(ClientCapabilities capabilities);

  /**
   * Creates a new request for sending a JSON-RPC message to the server. The returned ClientRequest can be configured and then sent by the caller.
   *
   * @return a future that completes with the client request
   */
  Future<ClientRequest> request();

  /**
   * Processes a JSON-RPC response received from the server and associates it with the corresponding session.
   *
   * @param session the session associated with the response, enabling bidirectional communication
   * @param response the JSON-RPC 2.0 response containing details such as result or error and linking to the original request
   * @return a future that will be completed once the response has been successfully processed or will fail if an error occurs
   */
  Future<Void> response(ClientSession session, JsonResponse response);

  /**
   * Sets a handler to process incoming JSON-RPC requests. The handler will be invoked whenever a {@link JsonRequest} is received.
   *
   * @param handler the handler to process received {@link JsonRequest} objects
   */
  void handler(Handler<JsonRequest> handler);
}
