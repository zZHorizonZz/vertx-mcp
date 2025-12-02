package io.vertx.mcp.client.transport.http;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.StreamResetException;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.internal.PromiseInternal;
import io.vertx.mcp.client.ClientRequest;
import io.vertx.mcp.client.ClientResponse;
import io.vertx.mcp.client.ClientSession;
import io.vertx.mcp.common.rpc.JsonRequest;

public class StreamableHttpClientRequest implements ClientRequest {

  private final ContextInternal context;
  private JsonRequest jsonRequest;
  private final HttpClientRequest httpRequest;
  private final ClientSession session;
  private final Future<ClientResponse> response;
  private boolean headersSent = false;

  public StreamableHttpClientRequest(HttpClientRequest httpRequest, ClientSession session) {
    this.context = ((PromiseInternal<?>) httpRequest.response()).context();
    this.httpRequest = httpRequest;
    this.session = session;
    this.response = httpRequest
      .response()
      .compose(httpResponse -> {
        String contentType = httpResponse.getHeader(HttpHeaders.CONTENT_TYPE);

        if (httpResponse.statusCode() != 200 && httpResponse.statusCode() != 202) {
          return Future.failedFuture("Invalid HTTP response status code: " + httpResponse.statusCode() + " " + httpResponse.statusMessage());
        }

        if (contentType == null || (!contentType.contains("application/json") && !contentType.contains("text/event-stream"))) {
          return Future.failedFuture("Invalid HTTP response content type: " + contentType);
        }

        StreamableHttpClientResponse mcpResponse = new StreamableHttpClientResponse(
          context,
          httpResponse,
          session,
          this
        );

        mcpResponse.init(session, this);

        return Future.succeededFuture(mcpResponse);
      }, err -> {
        if (err instanceof StreamResetException) {
          return Future.failedFuture("Stream was reset: " + err.getMessage());
        }
        return Future.failedFuture(err);
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
    return jsonRequest != null ? jsonRequest.getId() : null;
  }

  @Override
  public Future<ClientResponse> send(JsonRequest request) {
    if (headersSent) {
      return Future.failedFuture("Request already sent");
    }

    if (request == null) {
      return Future.failedFuture("Request cannot be null");
    }

    this.jsonRequest = request;

    setHeaders();

    Buffer requestBody = Buffer.buffer(request.toJson().encode());
    httpRequest.putHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(requestBody.length()));

    headersSent = true;

    return httpRequest.write(requestBody).compose(v -> httpRequest.end()).compose(v -> response);
  }

  @Override
  public Future<ClientResponse> response() {
    return response;
  }

  private void setHeaders() {
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
  }

  public Future<Void> cancel() {
    return httpRequest.reset();
  }

  public boolean isHeadersSent() {
    return headersSent;
  }

  public HttpClientRequest getHttpRequest() {
    return httpRequest;
  }
}

