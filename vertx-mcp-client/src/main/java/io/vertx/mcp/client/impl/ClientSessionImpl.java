package io.vertx.mcp.client.impl;

import io.vertx.core.Completable;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import io.vertx.mcp.client.ClientSession;
import io.vertx.mcp.common.capabilities.ServerCapabilities;
import io.vertx.mcp.common.request.Request;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientSessionImpl implements ClientSession {

  private final String id;
  private final boolean streaming;
  private final ServerCapabilities serverCapabilities;
  private final AtomicInteger requestCount = new AtomicInteger(0);
  private final AtomicBoolean active = new AtomicBoolean(true);
  private final Map<Object, Promise<JsonObject>> pendingRequests = new ConcurrentHashMap<>();

  private ReadStream<JsonObject> stream;

  public ClientSessionImpl(boolean streaming, ServerCapabilities serverCapabilities) {
    this(UUID.randomUUID().toString(), streaming, serverCapabilities);
  }

  public ClientSessionImpl(String id, boolean streaming, ServerCapabilities serverCapabilities) {
    this.id = id;
    this.streaming = streaming;
    this.serverCapabilities = serverCapabilities;
  }

  public void init(ReadStream<JsonObject> stream) {
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
  public ServerCapabilities serverCapabilities() {
    return this.serverCapabilities;
  }

  @Override
  public Future<JsonObject> sendRequest(Request request) {
    if (!active.get()) {
      return Future.failedFuture("Session is not active");
    }

    int requestId = requestCount.incrementAndGet();
    Promise<JsonObject> promise = Promise.promise();
    pendingRequests.put(requestId, promise);

    // The actual sending will be handled by the transport layer
    // This is a placeholder that needs to be integrated with the transport

    return promise.future();
  }

  @Override
  public boolean isStreaming() {
    return this.streaming;
  }

  @Override
  public boolean isActive() {
    return this.active.get();
  }

  @Override
  public void close(Completable<Void> completable) {
    active.set(false);

    pendingRequests.values().forEach(promise -> promise.fail("Session closed"));
    pendingRequests.clear();

    completable.succeed();
  }

  /**
   * Completes a pending request with the given result.
   *
   * @param requestId the request ID
   * @param result the result
   */
  public void completeRequest(Object requestId, JsonObject result) {
    Promise<JsonObject> promise = pendingRequests.remove(requestId);
    if (promise != null && !promise.future().isComplete()) {
      promise.complete(result);
    }
  }

  /**
   * Fails a pending request with the given error.
   *
   * @param requestId the request ID
   * @param error the error
   */
  public void failRequest(Object requestId, Throwable error) {
    Promise<JsonObject> promise = pendingRequests.remove(requestId);
    if (promise != null && !promise.future().isComplete()) {
      promise.fail(error);
    }
  }
}
