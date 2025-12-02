package io.vertx.mcp.client.transport.http;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.mcp.client.ClientOptions;
import io.vertx.mcp.client.ClientRequest;
import io.vertx.mcp.client.ClientSession;
import io.vertx.mcp.client.ClientTransport;
import io.vertx.mcp.client.impl.ClientSessionImpl;
import io.vertx.mcp.common.Implementation;
import io.vertx.mcp.common.capabilities.ClientCapabilities;
import io.vertx.mcp.common.capabilities.ServerCapabilities;
import io.vertx.mcp.common.request.InitializeRequest;
import io.vertx.mcp.common.result.InitializeResult;
import io.vertx.mcp.common.rpc.JsonRequest;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * HTTP transport for MCP client that supports streaming via Server-Sent Events. Implements the Streamable HTTP transport as specified in the MCP specification. This class is
 * inspired by the Vert.x gRPC client implementation.
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
  private final AtomicInteger requestIdGenerator = new AtomicInteger(0);

  public StreamableHttpClientTransport(Vertx vertx, String baseUrl, ClientOptions clientOptions) {
    this(vertx, baseUrl, clientOptions, new HttpClientOptions());
  }

  public StreamableHttpClientTransport(Vertx vertx, String baseUrl, ClientOptions clientOptions, HttpClientOptions httpOptions) {
    this.vertx = vertx;
    this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    this.clientOptions = clientOptions;
    this.httpClient = vertx.createHttpClient(httpOptions);
  }

  @Override
  public Future<ClientSession> connect(ClientCapabilities capabilities) {
    Promise<ClientSession> promise = Promise.promise();

    // Create initialize request
    InitializeRequest initRequest = new InitializeRequest()
      .setProtocolVersion(DEFAULT_PROTOCOL_VERSION)
      .setClientInfo(new Implementation()
        .setName(clientOptions.getClientName())
        .setVersion(clientOptions.getClientVersion()))
      .setCapabilities(capabilities);

    JsonRequest jsonRequest = initRequest.toRequest(requestIdGenerator.incrementAndGet());

    // Per spec: "Every JSON-RPC message sent from the client MUST be a new HTTP POST request"
    // The initialize request is sent as a POST, and the server can respond with either
    // application/json or text/event-stream
    request().compose(request -> {
        // Set the initialize request and send it
        request.setJsonRequest(jsonRequest);

        // Send the request and get the response (which handles both JSON and SSE)
        // We need to use flatMap here to properly chain the futures
        return request.sendRequest();
      })
      .onSuccess(response -> {
        // Set up handler to process the initialization response
        response.handler(json -> {
          try {
            InitializeResult result = new InitializeResult(json);
            ServerCapabilities serverCaps = result.getCapabilities();

            // Create session
            ClientSessionImpl session = new ClientSessionImpl(
              clientOptions.getStreamingEnabled(),
              serverCaps
            );

            promise.complete(session);
          } catch (Exception e) {
            promise.fail(e);
          }
        });

        response.exceptionHandler(promise::fail);
      })
      .onFailure(promise::fail);

    return promise.future();
  }

  @Override
  public Future<ClientRequest> request() {
    return httpClient.request(new RequestOptions().setMethod(HttpMethod.POST).setAbsoluteURI(baseUrl)).map(httpRequest -> new StreamableHttpClientRequest(httpRequest, null));
  }

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
}

