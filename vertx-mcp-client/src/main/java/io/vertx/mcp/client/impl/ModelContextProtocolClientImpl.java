package io.vertx.mcp.client.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.client.*;
import io.vertx.mcp.common.capabilities.ClientCapabilities;
import io.vertx.mcp.common.notification.Notification;
import io.vertx.mcp.common.request.Request;
import io.vertx.mcp.common.result.Result;
import io.vertx.mcp.common.rpc.JsonCodec;
import io.vertx.mcp.common.rpc.JsonNotification;
import io.vertx.mcp.common.rpc.JsonRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class ModelContextProtocolClientImpl implements ModelContextProtocolClient {

  private final Vertx vertx;
  private final List<ClientFeature> features = new ArrayList<>();
  private final ClientOptions options;
  private final ClientTransport transport;

  private final AtomicInteger requestIdGenerator = new AtomicInteger(0);

  public ModelContextProtocolClientImpl(Vertx vertx, ClientTransport transport) {
    this(vertx, transport, new ClientOptions());
  }

  public ModelContextProtocolClientImpl(Vertx vertx, ClientTransport transport, ClientOptions options) {
    this.vertx = vertx;
    this.transport = transport;
    this.options = options;

    this.transport.handler(this::handle);
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
  public Future<ClientSession> connect(ClientCapabilities capabilities) {
    return transport.subscribe(capabilities);
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

  private void handle(JsonRequest request) {
    if(request instanceof JsonNotification) {
      }

    String method = request.getMethod();
    Optional<ClientFeature> feature = features.stream().filter(f -> f.hasCapability(method)).findFirst();
    if(feature.isEmpty()) {
      //promise.fail(new ClientException("No feature found for method: " + method));
      return;
    }

    feature.get().
  }
}
