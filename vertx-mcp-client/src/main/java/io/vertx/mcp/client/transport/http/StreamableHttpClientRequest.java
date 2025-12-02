package io.vertx.mcp.client.transport.http;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.internal.ContextInternal;
import io.vertx.mcp.client.ClientRequest;
import io.vertx.mcp.client.ClientResponse;
import io.vertx.mcp.client.ClientSession;
import io.vertx.mcp.common.rpc.JsonRequest;

/**
 * HTTP client request implementation for MCP client. Implements the ClientRequest interface for HTTP transport.
 */
public class StreamableHttpClientRequest implements ClientRequest {

  private final ContextInternal context;

  private final JsonRequest jsonRequest;
  private final HttpClientRequest httpRequest;

  private final ClientSession session;
  private final Future<ClientResponse> response;

  public StreamableHttpClientRequest(ContextInternal context, JsonRequest jsonRequest, HttpClientRequest httpRequest, ClientSession session) {
    this.context = context;
    this.jsonRequest = jsonRequest;
    this.httpRequest = httpRequest;
    this.session = session;
    this.response = httpRequest
      .response()
      .compose(httpResponse -> {

      });
  }

  @Override
  public String path() {
    return httpRequest.path();
  }

  @Override
  public ContextInternal context() {
    return context;
  }

  @Override
  public JsonRequest getJsonRequest() {
    return jsonRequest;
  }

  @Override
  public ClientSession session() {
    return session;
  }

  @Override
  public Object requestId() {
    return jsonRequest.getId();
  }

  /**
   * Sends the JSON-RPC request to the server.
   *
   * @return a future that completes with the response
   */
  public Future<Void> send() {
    // Set content type
    httpRequest.putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
    httpRequest.putHeader(HttpHeaders.ACCEPT, "application/json, text/event-stream");

    // Add session ID if available
    if (session != null) {
      httpRequest.putHeader(StreamableHttpClientTransport.MCP_SESSION_ID_HEADER, session.id());
    }

    httpRequest.putHeader(
      StreamableHttpClientTransport.MCP_PROTOCOL_VERSION_HEADER,
      StreamableHttpClientTransport.DEFAULT_PROTOCOL_VERSION
    );

    // Send the request
    return httpRequest.write(jsonRequest.toJson().encode());
  }

  /**
   * Gets the underlying HTTP client request.
   *
   * @return the HTTP client request
   */
  public HttpClientRequest getHttpRequest() {
    return httpRequest;
  }
}

