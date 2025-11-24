package io.vertx.mcp.server.impl;

import io.vertx.core.Completable;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.WriteStream;
import io.vertx.mcp.common.capabilities.ClientCapabilities;
import io.vertx.mcp.common.notification.Notification;
import io.vertx.mcp.common.request.Request;
import io.vertx.mcp.server.ServerSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerSessionImpl implements ServerSession {

  private final String id;
  private final boolean streaming;
  private final ClientCapabilities capabilities;
  private final AtomicInteger requestCount = new AtomicInteger(0);
  private final AtomicBoolean active = new AtomicBoolean(true);
  private final Map<Object, Promise<JsonObject>> pendingRequests = new ConcurrentHashMap<>();

  private WriteStream<JsonObject> stream;
  private volatile String minLogLevel;

  public ServerSessionImpl(String id, boolean streaming, ClientCapabilities capabilities) {
    this.id = id;
    this.streaming = streaming;
    this.capabilities = capabilities;
  }

  public void init(WriteStream<JsonObject> stream) {
    this.stream = stream;
  }

  public Map<Object, Promise<JsonObject>> getPendingRequests() {
    return pendingRequests;
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
  public Future<JsonObject> sendRequest(Request request) {
    if (!active.get()) {
      return Future.failedFuture("Session is not active");
    }

    if (!isStreaming()) {
      return Future.failedFuture("Session is not streaming");
    }

    int requestId = requestCount.incrementAndGet();
    Promise<JsonObject> promise = Promise.promise();
    pendingRequests.put(requestId, promise);

    this.stream.write(request.toRequest(requestId).toJson()).onFailure(err -> {
      pendingRequests.remove(requestId);
      promise.fail(err);
    });

    return promise.future();
  }

  @Override
  public Future<Void> sendNotification(Notification notification) {
    if (!active.get()) {
      return Future.failedFuture("Session is not active");
    }

    if (!isStreaming()) {
      return Future.failedFuture("Session is not streaming");
    }

    return this.stream.write(notification.toNotification().toJson());
  }

  @Override
  public void close(Completable<Void> completable) {
    if (active.compareAndSet(true, false)) {
      completable.succeed();
      return;
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

  @Override
  public String minLogLevel() {
    return minLogLevel;
  }

  @Override
  public ServerSession setMinLogLevel(String level) {
    this.minLogLevel = level;
    return this;
  }
}
