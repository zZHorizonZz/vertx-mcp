package io.vertx.mcp.client.impl;

import io.vertx.core.Vertx;
import io.vertx.mcp.client.ClientFeature;
import io.vertx.mcp.client.ClientOptions;
import io.vertx.mcp.client.ClientResponse;
import io.vertx.mcp.client.ModelContextProtocolClient;
import io.vertx.mcp.common.rpc.JsonError;
import io.vertx.mcp.common.rpc.JsonNotification;
import io.vertx.mcp.common.rpc.JsonResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ModelContextProtocolClientImpl implements ModelContextProtocolClient {

  private final Vertx vertx;
  private final List<ClientFeature> features = new ArrayList<>();
  private final ClientOptions options;

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

  /**
   * Gets the client options.
   *
   * @return the client options
   */
  public ClientOptions getOptions() {
    return options;
  }

  @Override
  public void handle(ClientResponse response) {
    try {
      JsonResponse jsonResponse = response.getJsonResponse();
      boolean isNotification = jsonResponse.getId() == null;

      if (isNotification && !options.getNotificationsEnabled()) {
        return;
      }

      // For notifications, we need to determine which feature should handle it
      // For regular responses, we need to match them to pending requests

      if (isNotification) {
        // Extract method from notification metadata if available
        // This is a simplified version - you may need to adjust based on your notification structure
        String method = extractMethodFromNotification(jsonResponse);
        if (method != null) {
          Optional<ClientFeature> feature = features.stream()
            .filter(f -> f.hasCapability(method))
            .findFirst();

          if (feature.isPresent()) {
            feature.get().handle(response);
          }
        }
      } else {
        // Handle regular response - delegate to appropriate feature based on the original request
        // This requires tracking pending requests, which would typically be done in a request manager
        Optional<ClientFeature> feature = findFeatureForResponse(response);
        if (feature.isPresent()) {
          feature.get().handle(response);
        }
      }
    } catch (Exception e) {
      // Log or handle the exception appropriately
      // In a real implementation, you'd want proper error handling here
    }
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

  /**
   * Extracts the method name from a notification response.
   * This is a helper method that should be implemented based on your notification structure.
   *
   * @param jsonResponse the JSON response
   * @return the method name, or null if not found
   */
  private String extractMethodFromNotification(JsonResponse jsonResponse) {
    // This is a placeholder - implement based on your notification structure
    // You might need to look at the result or error fields to determine the method
    return null;
  }

  /**
   * Finds the appropriate feature to handle a response.
   * This is a placeholder that should be implemented with proper request tracking.
   *
   * @param response the client response
   * @return the feature that should handle this response, if found
   */
  private Optional<ClientFeature> findFeatureForResponse(ClientResponse response) {
    // This is a placeholder - in a real implementation, you'd track pending requests
    // and match responses to the features that initiated the requests
    return Optional.empty();
  }
}
