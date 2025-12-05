package io.vertx.mcp.client.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.mcp.client.*;
import io.vertx.mcp.client.feature.ProtocolClientFeature;
import io.vertx.mcp.common.capabilities.ClientCapabilities;
import io.vertx.mcp.common.notification.Notification;
import io.vertx.mcp.common.request.Request;
import io.vertx.mcp.common.result.Result;
import io.vertx.mcp.common.rpc.JsonCodec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ModelContextProtocolClientImpl implements ModelContextProtocolClient {

  private final Vertx vertx;

  private final ClientOptions options;
  private final ClientTransport transport;

  private final List<ClientFeature> features = new ArrayList<>();
  private final Map<String, ClientNotificationHandler> notificationHandlers = new HashMap<>();

  private final AtomicInteger requestIdGenerator = new AtomicInteger(0);

  public ModelContextProtocolClientImpl(Vertx vertx, ClientTransport transport) {
    this(vertx, transport, new ClientOptions());
  }

  public ModelContextProtocolClientImpl(Vertx vertx, ClientTransport transport, ClientOptions options) {
    this.vertx = vertx;
    this.transport = transport;
    this.options = options;
    
    this.features.add(new ProtocolClientFeature());
  }

  @Override
  public ModelContextProtocolClient addClientFeature(ClientFeature feature) {
    if (this.features.stream().anyMatch(f -> feature.getCapabilities().stream().anyMatch(f::hasCapability))) {
      throw new IllegalStateException("Feature already registered for " + feature.getCapabilities());
    }

    this.features.add(feature);
    feature.init(this.vertx);

    return this;
  }

  @Override
  public List<ClientFeature> features() {
    return List.copyOf(features);
  }

  @Override
  public ModelContextProtocolClient addNotificationHandler(String notificationType, ClientNotificationHandler handler) {
    notificationHandlers.put(notificationType, handler);
    return this;
  }

  @Override
  public Map<String, ClientNotificationHandler> notificationHandlers() {
    return Map.copyOf(notificationHandlers);
  }

  @Override
  public Future<ClientSession> connect(ClientCapabilities capabilities) {
    return transport.subscribe(this, capabilities);
  }

  @Override
  public Future<ClientRequest> request() {
    return transport.request();
  }

  @Override
  public Future<ClientRequest> request(ClientSession session) {
    return transport.request(session);
  }

  @Override
  public Future<Result> sendRequest(Request request) {
    return sendRequest(request, null);
  }

  @Override
  public Future<Result> sendRequest(Request request, ClientSession session) {
    Promise<Result> promise = Promise.promise();

    return request(session)
      .compose(req -> req.end(request.toRequest(requestIdGenerator.incrementAndGet()))
        .compose(v -> req.response().onSuccess(resp -> {
          resp.handler(json -> promise.complete(JsonCodec.decodeResult(request.getMethod(), json.getJsonObject("result"))));
          resp.exceptionHandler(promise::fail);
        }))
      )
      .compose(v -> promise.future());
  }

  @Override
  public Future<Void> sendNotification(Notification notification, ClientSession session) {
    return request(session).compose(req -> req.end(notification.toNotification()));
  }
}
