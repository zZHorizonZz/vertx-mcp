package io.vertx.mcp.client;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.mcp.client.impl.ModelContextProtocolClientImpl;
import io.vertx.mcp.common.capabilities.ClientCapabilities;
import io.vertx.mcp.common.notification.Notification;
import io.vertx.mcp.common.request.Request;
import io.vertx.mcp.common.result.Result;

import java.util.List;

/**
 * The {@code ModelContextProtocolClient} interface defines a protocol client for managing Model Context Protocol operations. It extends the {@code Handler<ClientResponse>}
 * interface to handle client responses within the JSON-RPC framework. This client provides functionalities such as protocol handling, feature registration, and interaction with
 * server responses.
 */
@VertxGen
public interface ModelContextProtocolClient {

  /**
   * Creates a new instance of the ModelContextProtocolClient with default client options. This method initializes a protocol client that supports the core features of the Model
   * Context Protocol.
   *
   * @param vertx the Vert.x instance
   * @return a new instance of ModelContextProtocolClient with default configuration.
   */
  static ModelContextProtocolClient create(Vertx vertx, ClientTransport transport) {
    return create(vertx, transport, new ClientOptions());
  }

  /**
   * Creates a new instance of the ModelContextProtocolClient using the specified client name and client version.
   *
   * @param vertx the Vert.x instance
   * @param clientName the name of the client
   * @param clientVersion the version of the client
   * @return a new instance of ModelContextProtocolClient configured with the specified client name and version
   */
  static ModelContextProtocolClient create(Vertx vertx, ClientTransport transport, String clientName, String clientVersion) {
    return create(vertx, transport, new ClientOptions().setClientName(clientName).setClientVersion(clientVersion));
  }

  /**
   * Creates and returns a new instance of {@code ModelContextProtocolClient} with the specified client options.
   *
   * @param vertx the Vert.x instance
   * @param options the {@code ClientOptions} object containing configuration details for the client
   * @return a new instance of {@code ModelContextProtocolClient} initialized with the provided options
   */
  static ModelContextProtocolClient create(Vertx vertx, ClientTransport transport, ClientOptions options) {
    return new ModelContextProtocolClientImpl(vertx, transport, options);
  }

  /**
   * Adds a client feature to the Model Context Protocol client. Client features define specific capabilities for the client, such as handling tools, resources, or prompts.
   *
   * @param feature the client feature to add
   * @return the current instance of {@code ModelContextProtocolClient}, allowing for method chaining
   */
  @Fluent
  ModelContextProtocolClient addClientFeature(ClientFeature feature);

  /**
   * Retrieves the list of client features registered with the client. Each client feature represents a specific capability supported by the client, such as tools, resources, or
   * prompts.
   *
   * @return a list of registered client features
   */
  List<ClientFeature> features();

  /**
   * Connects to an MCP server via HTTP transport.
   *
   * @param capabilities the client capabilities to advertise
   * @return a future that completes with the established session
   */
  @GenIgnore
  Future<ClientSession> connect(ClientCapabilities capabilities);

  @GenIgnore
  Future<ClientRequest> request();

  @GenIgnore
  Future<ClientRequest> request(ClientSession session);

  Future<Result> sendRequest(Request request);

  Future<Result> sendRequest(Request request, ClientSession session);

  Future<Void> sendNotification(Notification notification, ClientSession session);
}
