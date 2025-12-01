package io.vertx.mcp.client.impl;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.mcp.client.ClientFeature;
import io.vertx.mcp.client.ClientOptions;
import io.vertx.mcp.client.ClientResponse;
import io.vertx.mcp.client.ClientSession;
import io.vertx.mcp.client.ModelContextProtocolClient;
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
        Optional<ClientFeature> feature = findFeatureForResponse(response);
        if (feature.isPresent()) {
          feature.get().handle(response);
        }
      }
    } catch (Exception e) {
      // Log or handle the exception appropriately
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
  public StreamableHttpClientTransport getTransport() {
    return transport;
  }

  /**
   * Extracts the method name from a notification response.
   * This is a helper method that should be implemented based on your notification structure.
   *
   * @param jsonResponse the JSON response
   * @return the method name, or null if not found
   */
  private String extractMethodFromNotification(JsonResponse jsonResponse) {
    // Extract method from the notification
    // In MCP, notifications typically have a "method" field in their structure
    if (jsonResponse.getResult() != null && jsonResponse.getResult().containsKey("method")) {
      return jsonResponse.getResult().getString("method");
    }
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
    // In a real implementation, you'd track pending requests
    // and match responses to the features that initiated the requests
    return Optional.empty();
  }
}
