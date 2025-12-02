package io.vertx.mcp.client;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.mcp.common.capabilities.ClientCapabilities;

/**
 * Transport layer for MCP client.
 */
@VertxGen
public interface ClientTransport {
  /**
   * Connects to the MCP server and performs the initialization handshake.
   *
   * @param capabilities the client capabilities
   * @return a future that completes with the initialized session
   */
  Future<ClientSession> connect(ClientCapabilities capabilities);

  /**
   * Creates a new request for sending a JSON-RPC message to the server.
   * The returned ClientRequest can be configured and then sent by the caller.
   *
   * @return a future that completes with the client request
   */
  Future<ClientRequest> request();
}
