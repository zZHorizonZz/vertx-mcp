package io.vertx.mcp.server.impl;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.server.Session;

import java.util.concurrent.atomic.AtomicBoolean;

public class SessionImpl implements Session {

  private final String id;
  private final boolean streaming;
  private final AtomicBoolean active = new AtomicBoolean(true);

  public SessionImpl(String id, boolean streaming) {
    this.id = id;
    this.streaming = streaming;
  }

  @Override
  public String id() {
    return id;
  }

  @Override
  public boolean isStreaming() {
    return streaming;
  }

  @Override
  public boolean isActive() {
    return active.get();
  }

  @Override
  public Future<Void> send(JsonResponse response) {
    if (!active.get()) {
      return Future.failedFuture("Session is not active");
    }

    if (!streaming) {
      return Future.failedFuture("Session is not streaming");
    }

    Vertx.vertx().eventBus().send("vertx.mcp.server.session.response", response.toJson());

    return Future.succeededFuture();
  }

  @Override
  public Future<Void> close() {
    if (active.compareAndSet(true, false)) {
      if (streaming) {
        Vertx.vertx().eventBus().send("vertx.mcp.server.session.close", id);
      }
    }

    return Future.succeededFuture();
  }
}
