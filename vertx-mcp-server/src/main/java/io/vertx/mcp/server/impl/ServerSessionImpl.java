package io.vertx.mcp.server.impl;

import io.vertx.core.Completable;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.WriteStream;
import io.vertx.mcp.common.capabilities.ClientCapabilities;
import io.vertx.mcp.common.notification.Notification;
import io.vertx.mcp.common.request.Request;
import io.vertx.mcp.common.result.Result;
import io.vertx.mcp.server.ServerSession;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerSessionImpl implements ServerSession {

  private final String id;
  private final boolean streaming;
  private final ClientCapabilities capabilities;
  private final AtomicInteger requestCount = new AtomicInteger(0);
  private final AtomicBoolean active = new AtomicBoolean(true);
  //private final Map<Object, >

  private WriteStream<JsonObject> stream;

  public ServerSessionImpl(String id, boolean streaming, ClientCapabilities capabilities) {
    this.id = id;
    this.streaming = streaming;
    this.capabilities = capabilities;
  }

  public void init(WriteStream<JsonObject> stream) {
    this.stream = stream;
  }

  @Override
  public String id() {
    return this.id;
  }

  @Override
  public ClientCapabilities clientCapabilities() {
    return this.capabilities;
  }

  @Override
  public Future<Result> sendRequest(Request request) {
    if (!active.get()) {
      return Future.failedFuture("ServerSession is not active");
    }

    if (!streaming) {
      return Future.failedFuture("ServerSession is not streaming");
    }

    //TODO: How to handle this?
    return this.stream.write(request.toRequest(requestCount.incrementAndGet()).toJson()).map(v -> null);
  }

  @Override
  public Future<Void> sendNotification(Notification notification) {
    if (!active.get()) {
      return Future.failedFuture("ServerSession is not active");
    }

    if (!streaming) {
      return Future.failedFuture("ServerSession is not streaming");
    }

    return this.stream.write(notification.toNotification().toJson());
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

  @Override
  public boolean isStreaming() {
    return this.streaming && this.stream != null;
  }

  @Override
  public boolean isActive() {
    return active.get();
  }
}
