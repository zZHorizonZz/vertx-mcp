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
   * Subscribes a {@code ModelContextProtocolClient} to the server using the specified client capabilities. This method establishes a session between the client and the server,
   * utilizing the provided capabilities to negotiate supported features and configurations.
   *
   * @param client the protocol client used to communicate with the server
   * @param capabilities the client capabilities to advertise during the subscription process
   * @return a future that completes with the established {@code ClientSession}, which can be used for further communication with the server
   */
  default Future<ClientSession> subscribe(ModelContextProtocolClient client, ClientCapabilities capabilities) {
    return subscribe(client, capabilities, null);
  }

  /**
   * Subscribes the client to an MCP server with the specified client capabilities and session. This method allows establishing a connection or reusing an existing session for
   * communication.
   *
   * @param client the {@code ModelContextProtocolClient} instance used for protocol-specific operations
   * @param capabilities the capabilities that the client advertises to the server
   * @param session the {@code ClientSession} instance to be reused, or null to create a new session
   * @return a future that completes with the established {@code ClientSession} once the subscription is successful
   */
  Future<ClientSession> subscribe(ModelContextProtocolClient client, ClientCapabilities capabilities, ClientSession session);

  /**
   * Unsubscribes the specified {@code ClientSession} from the server. This method terminates the session and performs necessary cleanup to ensure the client is disconnected from
   * the server.
   *
   * @param session the {@code ClientSession} to be unsubscribed
   * @return a future that completes when the unsubscription process is finished
   */
  Future<Void> unsubscribe(ClientSession session);

  /**
   * Creates a new client request in the JSON-RPC framework. This method initiates the construction of a {@code ClientRequest}, which can be used to perform operations such as
   * sending requests and retrieving responses.
   *
   * @return a future that completes with the created {@code ClientRequest}
   */
  Future<ClientRequest> request();

  /**
   * Sends a request to the server within the context of the specified client session.
   *
   * @param session the client session to associate with the request; this session is used to identify the context for the server interaction
   * @return a future that completes with the {@code ClientRequest} representing the request sent to the server
   */
  Future<ClientRequest> request(ClientSession session);
}
