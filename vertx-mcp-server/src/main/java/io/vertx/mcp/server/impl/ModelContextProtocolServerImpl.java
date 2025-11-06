package io.vertx.mcp.server.impl;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.rpc.JsonError;
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

  private final List<ServerFeature> features = new ArrayList<>();
  private final ServerOptions options;

  /**
   * Creates a new MCP server instance with default options.
   */
  public ModelContextProtocolServerImpl() {
    this(new ServerOptions());
  }

  /**
   * Creates a new MCP server instance with specified options.
   *
   * @param options the server options
   */
  public ModelContextProtocolServerImpl(ServerOptions options) {
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
    // Set up a handler to read the JSON-RPC request and determine the method
    request.handler(jsonData -> {
      try {
        JsonRequest jsonRequest = JsonRequest.fromJson(jsonData);

        // Store the parsed JSON-RPC request in the ServerRequest
        request.setJsonRequest(jsonRequest);

        String method = jsonRequest.getMethod();

        // Check if this is a notification (no id means no response expected)
        boolean isNotification = jsonRequest.isNotification();

        // Check if notifications are enabled
        if (isNotification && !options.getNotificationsEnabled()) {
          // Notifications are disabled, ignore silently
          return;
        }

        // Find a feature that can handle this method
        Optional<ServerFeature> feature = features.stream()
          .filter(f -> f.hasCapability(method))
          .findFirst();

        if (feature.isEmpty()) {
          // No feature found
          if (isNotification) {
            // Notifications don't get error responses, just log and return 202 Accepted
            System.err.println("No handler for notification: " + method);
            request.response().endWithAccepted();
            return;
          }

          // For requests, return method not found error
          JsonResponse errorResponse = JsonResponse.error(
            jsonRequest,
            JsonError.methodNotFound(method)
          );
          request.response().end(errorResponse);
          return;
        }

        // For notifications, send 202 Accepted immediately and handle in background
        if (isNotification) {
          request.response().endWithAccepted();
          // Process the notification asynchronously (fire and forget)
          Handler<ServerRequest> handler = feature.get();
          handler.handle(request);
        } else {
          // For requests, call the feature handler which will send the response
          Handler<ServerRequest> handler = feature.get();
          handler.handle(request);
        }

      } catch (Exception e) {
        // Failed to parse the request - return invalid request error
        JsonResponse errorResponse = new JsonResponse(
          JsonError.invalidRequest(e.getMessage()),
          null
        );
        request.response().end(errorResponse);
      }
    });
  }

  @Override
  public ModelContextProtocolServer serverFeatures(ServerFeature feature) {
    if (this.features.stream().anyMatch(f -> feature.getCapabilities().stream().anyMatch(f::hasCapability))) {
      throw new IllegalArgumentException("Feature already registered for " + feature.getCapabilities());
    }

    this.features.add(feature);

    return this;
  }

  @Override
  public List<ServerFeature> features() {
    return List.copyOf(features);
  }
}
