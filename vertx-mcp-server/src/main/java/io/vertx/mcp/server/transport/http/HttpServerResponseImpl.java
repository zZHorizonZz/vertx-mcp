package io.vertx.mcp.server.transport.http;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.WriteStream;
import io.vertx.mcp.common.rpc.JsonBatch;
import io.vertx.mcp.server.ServerResponse;
import io.vertx.mcp.server.Session;

public class HttpServerResponseImpl implements ServerResponse {

  private final ContextInternal context;
  private final HttpServerResponse httpResponse;
  private Handler<Throwable> exceptionHandler;

  private boolean ended = false;
  private Session session;

  public HttpServerResponseImpl(ContextInternal context, HttpServerResponse httpResponse) {
    this.context = context;
    this.httpResponse = httpResponse;
  }

  @Override
  public void init() {

  }

  @Override
  public ContextInternal context() {
    return context;
  }

  @Override
  public Future<Void> write(JsonObject data) {
    if (ended) {
      return Future.failedFuture("Response already ended");
    }

    if (this.session != null && this.session.isStreaming()) {
      return httpResponse.write("data: " + data.encode() + "\n\n");
    }

    return end(data);
  }

  @Override
  public Future<Void> end() {
    if (ended) {
      return Future.succeededFuture();
    }
    ended = true;
    return httpResponse.end();
  }

  @Override
  public Future<Void> end(JsonObject data) {
    if (ended) {
      return Future.failedFuture("Response already ended");
    }

    ended = true;

    // If this is a new session (from initialize), set the session ID header
    if (session != null) {
      httpResponse.putHeader(HttpServerTransport.MCP_SESSION_ID_HEADER, session.id());
    }

    httpResponse.setStatusCode(200);
    httpResponse.putHeader(HttpHeaders.CONTENT_TYPE, "application/json");

    return httpResponse.end(data.toBuffer());
  }

  @Override
  public Future<Void> end(JsonBatch batch) {
    if (ended) {
      return Future.failedFuture("Response already ended");
    }
    ended = true;
    httpResponse.putHeader("Content-Type", "application/json");
    return httpResponse.end(batch.toJson().toBuffer());
  }

  @Override
  public Future<Void> endWithAccepted() {
    if (ended) {
      return Future.failedFuture("Response already ended");
    }
    ended = true;

    // For notifications, we don't use SSE even if it's enabled
    // Reset headers to plain 202 Accepted
    httpResponse.setStatusCode(202);
    // Clear SSE headers if they were set
    httpResponse.headers().remove("Content-Type");
    httpResponse.headers().remove("Cache-Control");
    httpResponse.headers().remove("Connection");
    httpResponse.setChunked(false);

    return httpResponse.end();
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

  @Override
  public Session session() {
    return session;
  }

  @Override
  public void setSession(Session session) {
    this.session = session;
  }
}
