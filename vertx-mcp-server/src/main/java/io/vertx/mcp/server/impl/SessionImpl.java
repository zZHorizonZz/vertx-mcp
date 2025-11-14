package io.vertx.mcp.server.impl;

import io.vertx.core.Closeable;
import io.vertx.core.Completable;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.WriteStream;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.server.Session;

import java.util.concurrent.atomic.AtomicBoolean;

public class SessionImpl implements Session, Closeable {

  private final String id;
  private final boolean streaming;
  private final AtomicBoolean active = new AtomicBoolean(true);

  private WriteStream<JsonObject> stream;

  public SessionImpl(String id, boolean streaming) {
    this.id = id;
    this.streaming = streaming;
  }

  public void init(WriteStream<JsonObject> stream) {
    this.stream = stream;
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

    return this.stream.write(response.toJson());
  }

  @Override
  public void close(Completable<Void> completable) {
    if (active.compareAndSet(true, false)) {
      if (streaming) {
        Vertx.vertx().eventBus().send("vertx.mcp.server.session.close", id);
      }
    }

    this.stream.end().onComplete(completable);
  }
}
