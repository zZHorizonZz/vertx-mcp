package io.vertx.mcp.client.transport.http;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.client.ClientOptions;
import io.vertx.mcp.client.ClientSession;
import io.vertx.mcp.client.ClientTransport;
import io.vertx.mcp.client.ModelContextProtocolClient;
import io.vertx.mcp.client.impl.ClientSessionImpl;
import io.vertx.mcp.common.capabilities.ClientCapabilities;
import io.vertx.mcp.common.capabilities.ServerCapabilities;
import io.vertx.mcp.common.request.InitializeRequest;
import io.vertx.mcp.common.result.InitializeResult;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;

/**
 * HTTP transport for MCP client that supports streaming via Server-Sent Events. Implements the Streamable HTTP transport as specified in the MCP specification.
 *
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/basic/transports#streamable-http">Client Transports - Streamable HTTP</a>
 */
public class StreamableHttpClientTransport implements ClientTransport {

  public static final String MCP_SESSION_ID_HEADER = "Mcp-Session-Id";
  public static final String MCP_PROTOCOL_VERSION_HEADER = "Mcp-Protocol-Version";
  public static final String DEFAULT_PROTOCOL_VERSION = "2025-06-18";

  private final Vertx vertx;
  private final HttpClient httpClient;
  private final ClientOptions clientOptions;
  private final String baseUrl;
  private final ModelContextProtocolClient client;

  public StreamableHttpClientTransport(Vertx vertx, String baseUrl, ModelContextProtocolClient client, ClientOptions clientOptions) {
    this(vertx, baseUrl, client, clientOptions, new HttpClientOptions());
  }

  public StreamableHttpClientTransport(Vertx vertx, String baseUrl, ModelContextProtocolClient client, ClientOptions clientOptions, HttpClientOptions httpOptions) {
    this.vertx = vertx;
    this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    this.client = client;
    this.clientOptions = clientOptions;
    this.httpClient = vertx.createHttpClient(httpOptions);
  }

  /**
   * Connects to the MCP server and performs the initialization handshake.
   *
   * @param capabilities the client capabilities
   * @return a future that completes with the initialized session
   */
  public Future<ClientSession> connect(ClientCapabilities capabilities) {
    Promise<ClientSession> promise = Promise.promise();

    // Create initialize request
    InitializeRequest initRequest = new InitializeRequest()
      .setProtocolVersion(DEFAULT_PROTOCOL_VERSION)
      .setClientInfo(new io.vertx.mcp.common.Implementation()
        .setName(clientOptions.getClientName())
        .setVersion(clientOptions.getClientVersion()))
      .setCapabilities(capabilities);

    // Send initialize request without session
    sendRequest("/mcp", initRequest.toRequest(1), null)
      .onSuccess(response -> {
        try {
          InitializeResult result = new InitializeResult(response.getResult());
          ServerCapabilities serverCaps = result.getCapabilities();

          // Create session
          ClientSessionImpl session = new ClientSessionImpl(
            clientOptions.getStreamingEnabled(),
            serverCaps
          );

          // If streaming is enabled, establish SSE connection
          if (clientOptions.getStreamingEnabled()) {
            establishStreamingConnection(session)
              .onSuccess(v -> promise.complete(session))
              .onFailure(promise::fail);
          } else {
            promise.complete(session);
          }
        } catch (Exception e) {
          promise.fail(e);
        }
      })
      .onFailure(promise::fail);

    return promise.future();
  }

  /**
   * Sends a request to the server.
   *
   * @param path the request path
   * @param request the JSON-RPC request
   * @param session the session (may be null for initialize)
   * @return a future that completes with the JSON-RPC response
   */
  public Future<JsonResponse> sendRequest(String path, JsonRequest request, ClientSession session) {
    Promise<JsonResponse> promise = Promise.promise();

    String fullUrl = baseUrl + path;

    httpClient.request(HttpMethod.POST, fullUrl)
      .compose(httpRequest -> {
        // Set headers
        httpRequest.putHeader("Content-Type", "application/json");
        httpRequest.putHeader("Accept", "application/json");
        httpRequest.putHeader(MCP_PROTOCOL_VERSION_HEADER, DEFAULT_PROTOCOL_VERSION);

        if (session != null) {
          httpRequest.putHeader(MCP_SESSION_ID_HEADER, session.id());
        }

        // Send request
        return httpRequest.send(request.toJson().encode())
          .compose(HttpClientResponse::body);
      })
      .onSuccess(body -> {
        try {
          JsonObject json = new JsonObject(body);
          JsonResponse response = JsonResponse.fromJson(json);
          promise.complete(response);
        } catch (Exception e) {
          promise.fail("Failed to parse response: " + e.getMessage());
        }
      })
      .onFailure(promise::fail);

    return promise.future();
  }

  /**
   * Establishes a streaming (SSE) connection with the server.
   *
   * @param session the session
   * @return a future that completes when the connection is established
   */
  private Future<Void> establishStreamingConnection(ClientSessionImpl session) {
    Promise<Void> promise = Promise.promise();

    String fullUrl = baseUrl + "/mcp";

    httpClient.request(HttpMethod.GET, fullUrl)
      .compose(httpRequest -> {
        // Set headers for SSE
        httpRequest.putHeader("Accept", "text/event-stream");
        httpRequest.putHeader(MCP_PROTOCOL_VERSION_HEADER, DEFAULT_PROTOCOL_VERSION);
        httpRequest.putHeader(MCP_SESSION_ID_HEADER, session.id());

        return httpRequest.send();
      })
      .onSuccess(httpResponse -> {
        if (httpResponse.statusCode() == 200) {
          // Handle SSE stream
          StreamableHttpClientResponse sseHandler = new StreamableHttpClientResponse(
            (ContextInternal) vertx.getOrCreateContext(),
            httpResponse,
            session,
            client
          );

          httpResponse.handler(sseHandler);
          promise.complete();
        } else {
          promise.fail("Failed to establish streaming connection: " + httpResponse.statusCode());
        }
      })
      .onFailure(promise::fail);

    return promise.future();
  }

  /**
   * Closes a session.
   *
   * @param session the session to close
   * @return a future that completes when the session is closed
   */
  public Future<Void> closeSession(ClientSession session) {
    Promise<Void> promise = Promise.promise();

    String fullUrl = baseUrl + "/mcp";

    httpClient.request(HttpMethod.DELETE, fullUrl)
      .compose(httpRequest -> {
        httpRequest.putHeader(MCP_SESSION_ID_HEADER, session.id());
        httpRequest.putHeader(MCP_PROTOCOL_VERSION_HEADER, DEFAULT_PROTOCOL_VERSION);
        return httpRequest.send();
      })
      .onSuccess(httpResponse -> session.close(promise))
      .onFailure(err -> session.close(promise));

    return promise.future();
  }

  /**
   * Closes the HTTP client.
   *
   * @return a future that completes when the client is closed
   */
  public Future<Void> close() {
    return httpClient.close();
  }
}

