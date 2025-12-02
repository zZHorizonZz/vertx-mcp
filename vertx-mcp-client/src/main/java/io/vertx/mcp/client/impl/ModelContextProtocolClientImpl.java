package io.vertx.mcp.client.impl;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.RequestOptions;
import io.vertx.mcp.client.*;
import io.vertx.mcp.client.transport.http.StreamableHttpClientTransport;
import io.vertx.mcp.common.capabilities.ClientCapabilities;
import io.vertx.mcp.common.rpc.JsonResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ModelContextProtocolClientImpl implements ModelContextProtocolClient {

  private final Vertx vertx;
  private final List<ClientFeature> features = new ArrayList<>();
  private final ClientOptions options;
  private StreamableHttpClientTransport transport;

  /**
   * Creates a new MCP client instance with default options.
   */
  public ModelContextProtocolClientImpl(Vertx vertx) {
    this(vertx, new ClientOptions());
  }

  /**
   * Creates a new MCP client instance with specified options.
   *
   * @param options the client options
   */
  public ModelContextProtocolClientImpl(Vertx vertx, ClientOptions options) {
    this.vertx = vertx;
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
    // Create transport
    this.transport = new StreamableHttpClientTransport(vertx, baseUrl, this, options, httpOptions);

    // Connect to server
    return transport.connect(capabilities);
  }

  @Override
  public Future<ClientRequest> request() {
    return transport.request();
  }

  @Override
  public StreamableHttpClientTransport getTransport() {
    return transport;
  }
}
