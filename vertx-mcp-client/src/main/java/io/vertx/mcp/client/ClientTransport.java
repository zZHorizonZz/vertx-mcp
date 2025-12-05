package io.vertx.mcp.client;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.mcp.common.capabilities.ClientCapabilities;

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
  Future<ClientSession> subscribe(ModelContextProtocolClient client, ClientCapabilities capabilities);

  /**
   * Creates a new sendRequest for sending a JSON-RPC message to the server. The returned ClientRequest can be configured and then sent by the caller.
   *
   * @return a future that completes with the client sendRequest
   */
  Future<ClientRequest> request();

  Future<ClientRequest> request(ClientSession session);
}
