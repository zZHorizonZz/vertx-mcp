package io.vertx.mcp.server.impl;

import io.vertx.core.Vertx;
import io.vertx.mcp.common.rpc.JsonError;
import io.vertx.mcp.common.rpc.JsonNotification;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.server.ModelContextProtocolServer;
import io.vertx.mcp.server.ServerFeature;
import io.vertx.mcp.server.ServerOptions;
import io.vertx.mcp.server.ServerRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ModelContextProtocolServerImpl implements ModelContextProtocolServer {

  private final Vertx vertx;
  private final List<ServerFeature> features = new ArrayList<>();
  private final ServerOptions options;

  /**
   * Creates a new MCP server instance with default options.
   */
  public ModelContextProtocolServerImpl(Vertx vertx) {
    this(vertx, new ServerOptions());
  }

  /**
   * Creates a new MCP server instance with specified options.
   *
   * @param options the server options
   */
  public ModelContextProtocolServerImpl(Vertx vertx, ServerOptions options) {
    this.vertx = vertx;
    this.options = options;
  }

  /**
   * Gets the server options.
   *
   * @return the server options
   */
  public ServerOptions getOptions() {
    return options;
  }

  @Override
  public void handle(ServerRequest request) {
    try {
      JsonRequest jsonRequest = request.getJsonRequest();
      String method = jsonRequest.getMethod();
      boolean isNotification = jsonRequest instanceof JsonNotification;

      if (isNotification && !options.getNotificationsEnabled()) {
        request.response().end();
        return;
      }

      Optional<ServerFeature> feature = features.stream().filter(f -> f.hasCapability(method)).findFirst();

      if (feature.isEmpty()) {
        request.response().end(JsonResponse.error(jsonRequest, JsonError.methodNotFound(method)));
        return;
      }

      if (isNotification) {
        request.response().end();
        feature.get().handle(request);
      }

      feature.get().handle(request);
    } catch (Exception e) {
      request.response().end(new JsonResponse(JsonError.invalidRequest(e.getMessage()), null));
    }
  }

  @Override
  public ModelContextProtocolServer addServerFeature(ServerFeature feature) {
    if (this.features.stream().anyMatch(f -> feature.getCapabilities().stream().anyMatch(f::hasCapability))) {
      throw new IllegalStateException("Feature already registered for " + feature.getCapabilities());
    }

    this.features.add(feature);

    feature.init(this.vertx);

    return this;
  }

  @Override
  public List<ServerFeature> features() {
    return List.copyOf(features);
  }
}
