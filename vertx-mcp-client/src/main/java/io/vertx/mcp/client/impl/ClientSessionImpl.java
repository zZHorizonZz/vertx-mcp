package io.vertx.mcp.client.impl;

import io.vertx.core.Completable;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import io.vertx.mcp.client.*;
import io.vertx.mcp.common.capabilities.ServerCapabilities;
import io.vertx.mcp.common.notification.Notification;
import io.vertx.mcp.common.request.Request;
import io.vertx.mcp.common.result.Result;
import io.vertx.mcp.common.rpc.JsonCodec;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientSessionImpl implements ClientSession {

  private final String id;
  private final ServerCapabilities serverCapabilities;

  private final ClientTransport transport;
  private final AtomicInteger requestCount = new AtomicInteger(0);
  private final AtomicBoolean active = new AtomicBoolean(true);

  private final List<ClientFeature> featureHandlers;
  private final Map<String, ClientNotificationHandler> notificationHandlers;
  private final Map<Object, Promise<JsonObject>> pendingRequests = new ConcurrentHashMap<>();

  private ReadStream<JsonObject> readStream;

  public ClientSessionImpl(String id, ServerCapabilities serverCapabilities, ClientTransport transport, List<ClientFeature> featureHandlers,
    Map<String, ClientNotificationHandler> notificationHandlers) {
    this.id = id;
    this.serverCapabilities = serverCapabilities;
    this.transport = transport;
    this.featureHandlers = featureHandlers;
    this.notificationHandlers = notificationHandlers;
  }

  public void init(ReadStream<JsonObject> readStream) {
    this.readStream = readStream;
    readStream.handler(this);
  }

  @Override
  public void handle(JsonObject request) {
    String method = request.getString("method");

    if (method == null) {
      throw new IllegalArgumentException("Request must contain a method");
    }

    Optional<ClientFeature> handler = featureHandlers.stream().filter(h -> h.hasCapability(method)).findFirst();
    if (handler.isPresent()) {
      handler.get().apply(new JsonRequest(request))
        .onSuccess(result -> completeRequest(request.getString("id"), result))
        .onFailure(err -> failRequest(request.getString("id"), err));
      return;
    }

    Optional<ClientNotificationHandler> notificationHandler = Optional.ofNullable(notificationHandlers.get(method));

    if (notificationHandler.isPresent()) {
      notificationHandler.get().handle(JsonCodec.decodeNotification(method, request.getJsonObject("params")));
      return;
    }
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
  public Future<Result> sendRequest(Request request) {
    if (!active.get()) {
      return Future.failedFuture("Session is not active");
    }

    Promise<Result> promise = Promise.promise();

    return transport.request(this).compose(req -> req.end(request.toRequest(requestCount.incrementAndGet())).compose(v -> req.response())
      .onSuccess(resp -> resp
        .handler(response -> {
          JsonResponse jsonResponse = JsonResponse.fromJson(response);
          if (jsonResponse.getError() != null) {
            promise.fail(new ClientRequestException(jsonResponse.getError()));
          } else {
            promise.complete(JsonCodec.decodeResult(request.getMethod(), (JsonObject) jsonResponse.getResult()));
          }
        })
        .exceptionHandler(promise::fail)
      )
      .compose(v -> promise.future()));
  }

  @Override
  public Future<Void> sendNotification(Notification notification) {
    if (!active.get()) {
      return Future.failedFuture("Session is not active");
    }

    return transport.request(this).compose(req -> req.end(notification.toNotification()).mapEmpty());
  }

  @Override
  public boolean isStreaming() {
    //TODO: In future have a way to detect if the stream is actually streaming
    return this.readStream != null;
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

    transport.unsubscribe(this).onComplete(completable);
  }

  private void completeRequest(Object requestId, JsonObject result) {
    Promise<JsonObject> promise = pendingRequests.remove(requestId);
    if (promise != null && !promise.future().isComplete()) {
      promise.complete(result);
    }
  }

  private void failRequest(Object requestId, Throwable error) {
    Promise<JsonObject> promise = pendingRequests.remove(requestId);
    if (promise != null && !promise.future().isComplete()) {
      promise.fail(error);
    }
  }
}
