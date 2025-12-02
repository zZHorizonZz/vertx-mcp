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

/**
 * HTTP client request implementation for MCP client. Implements the ClientRequest interface for HTTP transport. This class is inspired by the Vert.x gRPC client implementation.
 */
public class StreamableHttpClientRequest implements ClientRequest {

  private final ContextInternal context;
  private JsonRequest jsonRequest;
  private final HttpClientRequest httpRequest;
  private final ClientSession session;
  private Future<ClientResponse> response;
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

        // Validate content type
        if (contentType == null || (!contentType.contains("application/json") && !contentType.contains("text/event-stream"))) {
          httpResponse.request().reset();
          return context.failedFuture("Invalid HTTP response content-type header: " + contentType);
        }

        // Create the StreamableHttpClientResponse
        StreamableHttpClientResponse mcpResponse = new StreamableHttpClientResponse(
          context,
          httpResponse,
          session,
          this
        );

        // Initialize the response
        mcpResponse.init(session, this);

        return Future.succeededFuture(mcpResponse);
      }, err -> {
        // Handle stream reset exceptions
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

  /**
   * Sets the JSON-RPC request to send.
   *
   * @param jsonRequest the JSON-RPC request (must have an ID already set)
   * @return this request for fluent API
   */
  public StreamableHttpClientRequest setJsonRequest(JsonRequest jsonRequest) {
    if (headersSent) {
      throw new IllegalStateException("Request already sent");
    }
    this.jsonRequest = jsonRequest;
    return this;
  }

  /**
   * Sends the JSON-RPC request to the server.
   *
   * @return a future that completes when the request has been sent
   */
  public Future<Void> send() {
    if (headersSent) {
      return Future.failedFuture("Request already sent");
    }

    if (jsonRequest == null) {
      return Future.failedFuture("JSON-RPC request not set. Call setJsonRequest() before sending.");
    }

    setHeaders();

    Buffer requestBody = Buffer.buffer(jsonRequest.toJson().encode());
    httpRequest.putHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(requestBody.length()));

    headersSent = true;

    return httpRequest.write(requestBody);
  }

  /**
   * Sends the request and returns the response future.
   *
   * @return a future that completes with the client response
   */
  public Future<ClientResponse> sendRequest() {
    return send().compose(v -> {
      if (response == null) {
        return Future.failedFuture("Response not available");
      }
      return response;
    });
  }

  /**
   * Gets the response future. The response future is only available after calling send().
   *
   * @return the response future, or null if send() has not been called yet
   */
  public Future<ClientResponse> response() {
    return response;
  }

  /**
   * Sets the HTTP headers for the request.
   */
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

  /**
   * Cancels the request by resetting the HTTP stream.
   *
   * @return a future that completes when the request is cancelled
   */
  public Future<Void> cancel() {
    return httpRequest.reset();
  }

  /**
   * Checks if the request headers have been sent.
   *
   * @return true if headers have been sent
   */
  public boolean isHeadersSent() {
    return headersSent;
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

