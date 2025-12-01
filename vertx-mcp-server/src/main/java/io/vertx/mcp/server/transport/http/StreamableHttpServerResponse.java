package io.vertx.mcp.server.transport.http;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.WriteStream;
import io.vertx.mcp.common.rpc.JsonProtocol;
import io.vertx.mcp.server.ServerResponse;
import io.vertx.mcp.server.ServerSession;

public class StreamableHttpServerResponse implements ServerResponse {

  private final ContextInternal context;
  private final HttpServerResponse httpResponse;
  private Handler<Throwable> exceptionHandler;

  private boolean ended = false;
  private Object requestId;
  private ServerSession session;

  public StreamableHttpServerResponse(ContextInternal context, HttpServerResponse httpResponse) {
    this.context = context;
    this.httpResponse = httpResponse;
  }

  @Override
  public void init(ServerSession session) {
    this.session = session;
  }

  @Override
  public Object requestId() {
    return this.requestId;
  }

  public void requestId(Object requestId) {
    this.requestId = requestId;
  }

  @Override
  public ServerSession session() {
    return this.session;
  }

  @Override
  public ContextInternal context() {
    return this.context;
  }

  @Override
  public Future<Void> write(JsonObject data) {
    if (ended) {
      return Future.failedFuture("Response already ended");
    }

    if (this.session != null && this.session.isStreaming()) {
      return httpResponse.write("data: " + data.encode() + "\n\n");
    }

    if (this.requestId != null && !data.containsKey(JsonProtocol.ID_FIELD)) {
      data.put(JsonProtocol.ID_FIELD, this.requestId);
    }

    return end(data);
  }

  @Override
  public Future<Void> end() {
    if (ended) {
      return Future.succeededFuture();
    }

    ended = true;

    if (this.requestId == null && !this.httpResponse.ended()) {
      httpResponse.setStatusCode(202);
      httpResponse.headers().remove(HttpHeaders.CONTENT_TYPE);
      httpResponse.headers().remove(HttpHeaders.CACHE_CONTROL);
      httpResponse.headers().remove(HttpHeaders.CONNECTION);
      httpResponse.setChunked(false);
    }

    return httpResponse.end();
  }

  @Override
  public Future<Void> end(JsonObject data) {
    if (ended) {
      return Future.failedFuture("Response already ended");
    }

    ended = true;

    // If response headers already written (SSE stream active), write as SSE and close
    if (httpResponse.headWritten()) {
      return httpResponse.write("data: " + data.encode() + "\n\n")
        .compose(v -> httpResponse.end());
    }

    // Non-streaming response - set headers and send JSON
    if (session != null) {
      httpResponse.putHeader(StreamableHttpServerTransport.MCP_SESSION_ID_HEADER, session.id());
    }

    httpResponse.setStatusCode(200);
    httpResponse.putHeader(HttpHeaders.CONTENT_TYPE, "application/json");

    return httpResponse.end(data.toBuffer());
  }

  @Override
  public WriteStream<JsonObject> setWriteQueueMaxSize(int maxSize) {
    httpResponse.setWriteQueueMaxSize(maxSize);
    return this;
  }

  @Override
  public boolean writeQueueFull() {
    return httpResponse.writeQueueFull();
  }

  @Override
  public WriteStream<JsonObject> drainHandler(@Nullable Handler<Void> handler) {
    httpResponse.drainHandler(handler);
    return this;
  }

  @Override
  public WriteStream<JsonObject> exceptionHandler(@Nullable Handler<Throwable> handler) {
    this.exceptionHandler = handler;
    httpResponse.exceptionHandler(handler);
    return this;
  }
}
