package io.vertx.mcp.client.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.mcp.client.*;
import io.vertx.mcp.common.capabilities.ClientCapabilities;
import io.vertx.mcp.common.request.Request;
import io.vertx.mcp.common.result.Result;
import io.vertx.mcp.common.rpc.JsonCodec;

import java.util.ArrayList;
import java.util.List;
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
  public Future<ClientSession> connect(String baseUrl, ClientCapabilities capabilities) {
    return connect(baseUrl, capabilities, new HttpClientOptions());
  }

  @Override
  public Future<ClientSession> connect(String baseUrl, ClientCapabilities capabilities, HttpClientOptions httpOptions) {
    return transport.connect(capabilities);
  }

  @Override
  public Future<ClientRequest> request() {
    return transport.request();
  }

  @Override
  public Future<Result> request(Request request) {
    Promise<Result> promise = Promise.promise();
    return request()
      .compose(clientRequest -> clientRequest.response().onSuccess(response -> {
        response.handler(json -> promise.complete(JsonCodec.decodeResult(request.getMethod(), json.getJsonObject("result"))));
        response.exceptionHandler(promise::fail);
      }).map(v -> clientRequest))
      .compose(clientRequest -> clientRequest.send(request.toRequest(requestIdGenerator.incrementAndGet())).compose(v -> clientRequest.end()))
      .compose(v -> promise.future());
  }
}
