package io.vertx.mcp.client.transport.http;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.core.internal.ContextInternal;
import io.vertx.mcp.client.*;
import io.vertx.mcp.client.impl.ClientSessionImpl;
import io.vertx.mcp.common.Implementation;
import io.vertx.mcp.common.capabilities.ClientCapabilities;
import io.vertx.mcp.common.capabilities.ServerCapabilities;
import io.vertx.mcp.common.request.InitializeRequest;
import io.vertx.mcp.common.result.InitializeResult;

import java.util.Optional;
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
  public Future<ClientSession> subscribe(ModelContextProtocolClient client, ClientCapabilities capabilities, ClientSession session) {
    if (session != null) {
      //TODO: Implement session re-use
      return Future.succeededFuture(session);
    }

    return initialize(client, capabilities).compose(s -> httpClient
      .request(new RequestOptions().setMethod(HttpMethod.GET).setAbsoluteURI(baseUrl).addHeader(MCP_SESSION_ID_HEADER, s.id()))
      .map(httpRequest -> {
        ClientRequest request = new StreamableHttpClientRequest(httpRequest, 256 * 1024, true, null);
        configureTimeout(request);

        httpRequest.putHeader(HttpHeaders.ACCEPT, "text/event-stream");

        return request;
      })
      .compose(request -> ((StreamableHttpClientRequest) request).sendEnd().compose(v -> request.response()))
      .onSuccess(response -> ((ClientSessionImpl) s).init(response))
      .map(v -> s));
  }

  @Override
  public Future<Void> unsubscribe(ClientSession session) {
    return httpClient.request(HttpMethod.DELETE, baseUrl).compose(httpRequest -> {
      httpRequest.putHeader(MCP_SESSION_ID_HEADER, session.id());
      httpRequest.putHeader(MCP_PROTOCOL_VERSION_HEADER, DEFAULT_PROTOCOL_VERSION);
      return httpRequest.send();
    }).mapEmpty();
  }

  @Override
  public Future<ClientRequest> request() {
    return request(null);
  }

  @Override
  public Future<ClientRequest> request(ClientSession session) {
    return httpClient.request(new RequestOptions().setMethod(HttpMethod.POST).setAbsoluteURI(baseUrl))
      .map(httpRequest -> {
        ClientRequest request = new StreamableHttpClientRequest(httpRequest, 256 * 1024, true, session);
        configureTimeout(request);
        return request;
      });
  }

  private Future<ClientSession> initialize(ModelContextProtocolClient client, ClientCapabilities capabilities) {
    Promise<ClientSession> promise = Promise.promise();

    initialize(capabilities).onSuccess(response -> response.handler(json -> {
      try {
        InitializeResult result = new InitializeResult(json);
        ServerCapabilities serverCapabilities = result.getCapabilities();
        StreamableHttpClientResponse httpResponse = (StreamableHttpClientResponse) response;

        Optional<String> sessionId = httpResponse.headers().getAll(MCP_SESSION_ID_HEADER).stream().findFirst();
        if (sessionId.isEmpty()) {
          promise.fail(new IllegalStateException("No session ID returned from server"));
        }

        // Create session
        ClientSession session = new ClientSessionImpl(
          httpResponse.headers().get(MCP_SESSION_ID_HEADER),
          serverCapabilities,
          this,
          client.features(),
          client.notificationHandlers()
        );

        promise.complete(session);
      } catch (Exception e) {
        promise.fail(e);
      }
    })).onFailure(promise::fail);

    return promise.future();
  }

  private Future<ClientResponse> initialize(ClientCapabilities capabilities) {
    InitializeRequest initialize = new InitializeRequest()
      .setProtocolVersion(DEFAULT_PROTOCOL_VERSION)
      .setClientInfo(new Implementation()
        .setName(clientOptions.getClientName())
        .setVersion(clientOptions.getClientVersion())
      )
      .setCapabilities(capabilities);

    return request().compose(request -> request.end(initialize.toRequest(requestIdGenerator.incrementAndGet())).compose(v -> request.response()));
  }

  private void configureTimeout(ClientRequest request) {
    ContextInternal current = (ContextInternal) vertx.getOrCreateContext();
  }
}

