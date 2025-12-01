package io.vertx.mcp.client.transport.http;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.client.ClientRequest;
import io.vertx.mcp.client.ClientSession;
import io.vertx.mcp.common.rpc.JsonRequest;

/**
 * HTTP client request implementation for MCP client.
 * Implements the ClientRequest interface for HTTP transport.
 */
public class StreamableHttpClientRequest implements ClientRequest {

  private final ContextInternal context;
  private final HttpClientRequest httpRequest;
  private final JsonRequest jsonRequest;
  private ClientSession session;

  public StreamableHttpClientRequest(
    ContextInternal context,
    HttpClientRequest httpRequest,
    JsonRequest jsonRequest
  ) {
    this.context = context;
    this.httpRequest = httpRequest;
    this.jsonRequest = jsonRequest;
  }

  @Override
  public void init(ClientSession session) {
    this.session = session;
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
  public Future<JsonObject> send() {
    // Set content type
    httpRequest.putHeader("Content-Type", "application/json");
    httpRequest.putHeader("Accept", "application/json");

    // Add session ID if available
    if (session != null) {
      httpRequest.putHeader(StreamableHttpClientTransport.MCP_SESSION_ID_HEADER, session.id());
    }

    httpRequest.putHeader(
      StreamableHttpClientTransport.MCP_PROTOCOL_VERSION_HEADER,
      StreamableHttpClientTransport.DEFAULT_PROTOCOL_VERSION
    );

    // Send the request
    return httpRequest.send(jsonRequest.toJson().encode())
      .compose(response -> response.body())
      .map(body -> new JsonObject(body.toString()));
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

