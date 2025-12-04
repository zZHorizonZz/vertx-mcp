package io.vertx.mcp.client.transport.http;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.WriteStream;
import io.vertx.mcp.client.ClientSession;
import io.vertx.mcp.common.rpc.JsonRequest;

/**
 * Write stream for sending MCP results and notifications to the server via HTTP POST requests.
 * Each message (result or notification) is sent as a separate HTTP POST request using StreamableHttpClientRequest
 * for a unified approach with proper session ID and protocol headers.
 */
public class StreamableHttpClientSessionStream implements WriteStream<JsonObject> {

  private final ClientSession session;
  private final HttpClient client;
  private final String baseUrl;
  private Handler<Throwable> exceptionHandler;

  public StreamableHttpClientSessionStream(String sessionId, HttpClient client) {
    this(sessionId, client, "/mcp");
  }

  public StreamableHttpClientSessionStream(String sessionId, HttpClient client, String baseUrl) {
    this.session = new SimpleClientSession(sessionId);
    this.client = client;
    this.baseUrl = baseUrl;
  }

  @Override
  public WriteStream<JsonObject> exceptionHandler(@Nullable Handler<Throwable> handler) {
    this.exceptionHandler = handler;
    return this;
  }

  @Override
  public Future<Void> write(JsonObject message) {
    // Create a StreamableHttpClientRequest to send the JSON-RPC message
    // This ensures consistent handling of headers, session ID, and protocol version
    Future<Void> future = client.request(new RequestOptions().setMethod(HttpMethod.POST).setAbsoluteURI(baseUrl))
      .map(httpRequest -> new StreamableHttpClientRequest(httpRequest, 256 * 1024, true, session))
      .compose(request -> {
        // Create a JsonRequest wrapper for the message to use the unified sending approach
        JsonRequest jsonRequest = new JsonRequest(message);
        return request.end(jsonRequest);
      });

    // Handle exceptions if handler is set
    if (exceptionHandler != null) {
      future.onFailure(exceptionHandler);
    }

    return future;
  }

  @Override
  public Future<Void> end() {
    // For streaming, end() doesn't need to send anything - the session remains active
    return Future.succeededFuture();
  }

  @Override
  public WriteStream<JsonObject> setWriteQueueMaxSize(int maxSize) {
    // Not applicable for HTTP POST requests
    return this;
  }

  @Override
  public boolean writeQueueFull() {
    // Not applicable for HTTP POST requests - always ready to send
    return false;
  }

  @Override
  public WriteStream<JsonObject> drainHandler(@Nullable Handler<Void> handler) {
    // Not applicable for HTTP POST requests
    return this;
  }

  /**
   * Simple ClientSession implementation that only provides the session ID.
   * This is used internally by StreamableHttpClientRequest to add the session ID header.
   */
  private static class SimpleClientSession implements ClientSession {
    private final String sessionId;

    SimpleClientSession(String sessionId) {
      this.sessionId = sessionId;
    }

    @Override
    public String id() {
      return sessionId;
    }

    @Override
    public io.vertx.mcp.common.capabilities.ServerCapabilities serverCapabilities() {
      return null;
    }

    @Override
    public Future<JsonObject> sendRequest(io.vertx.mcp.common.request.Request request) {
      return Future.failedFuture("Not supported");
    }

    @Override
    public Future<Void> sendResult(io.vertx.mcp.common.request.Request request, io.vertx.mcp.common.result.Result result) {
      return Future.failedFuture("Not supported");
    }

    @Override
    public boolean isStreaming() {
      return true;
    }

    @Override
    public boolean isActive() {
      return true;
    }

    @Override
    public Future<Void> close() {
      return Future.succeededFuture();
    }
  }
}
