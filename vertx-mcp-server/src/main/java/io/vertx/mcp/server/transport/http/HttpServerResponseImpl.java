package io.vertx.mcp.server.transport.http;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.WriteStream;
import io.vertx.mcp.common.rpc.JsonBatch;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.server.ServerResponse;
import io.vertx.mcp.server.Session;

public class HttpServerResponseImpl implements ServerResponse {

  private static final String MCP_SESSION_ID_HEADER = "Mcp-Session-Id";

  private final ContextInternal context;
  private final HttpServerResponse httpResponse;
  private Handler<Throwable> exceptionHandler;
  private boolean ended = false;
  private Session session;
  private boolean newSession = false;

  public HttpServerResponseImpl(ContextInternal context, HttpServerResponse httpResponse) {
    this.context = context;
    this.httpResponse = httpResponse;
  }

  /**
   * Mark this response as having a new session that should be sent in the header.
   * This is called when a session is created during initialize.
   */
  public void markNewSession() {
    this.newSession = true;
  }

  @Override
  public void init() {
    httpResponse.putHeader("Content-Type", "application/json");
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
    return httpResponse.write(data.toBuffer());
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
  public Future<Void> end(JsonResponse response) {
    if (ended) {
      return Future.failedFuture("Response already ended");
    }
    ended = true;

    // If this is a new session (from initialize), set the session ID header
    if (newSession && session != null) {
      httpResponse.putHeader(MCP_SESSION_ID_HEADER, session.id());
    }

    // If session is in SSE mode, send via session instead
    if (session != null && session.isSse()) {
      return session.send(response);
    }

    httpResponse.putHeader("Content-Type", "application/json");
    return httpResponse.end(response.toJson().toBuffer());
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
    httpResponse.setStatusCode(202);
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
