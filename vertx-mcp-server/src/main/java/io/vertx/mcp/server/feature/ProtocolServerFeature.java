package io.vertx.mcp.server.feature;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.Implementation;
import io.vertx.mcp.common.capabilities.PromptsCapability;
import io.vertx.mcp.common.capabilities.ResourcesCapability;
import io.vertx.mcp.common.capabilities.ServerCapabilities;
import io.vertx.mcp.common.capabilities.ToolsCapability;
import io.vertx.mcp.common.notification.PromptListChangedNotification;
import io.vertx.mcp.common.notification.ResourceListChangedNotification;
import io.vertx.mcp.common.notification.ToolListChangedNotification;
import io.vertx.mcp.common.result.InitializeResult;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.server.ModelContextProtocolServer;
import io.vertx.mcp.server.ServerFeature;
import io.vertx.mcp.server.ServerOptions;
import io.vertx.mcp.server.ServerRequest;
import io.vertx.mcp.server.impl.ServerFeatureBase;

import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * The ProtocolServerFeature class implements the ServerFeatureBase and provides functionality to handle core MCP protocol operations like initialize and ping. This is a required
 * feature that manages protocol-level communication.
 *
 * @version 2025-06-18
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/basic/lifecycle">Server Features - Lifecycle</a>
 */
public class ProtocolServerFeature extends ServerFeatureBase {

  @Override
  public Map<String, BiFunction<ServerRequest, JsonRequest, Future<JsonResponse>>> getHandlers() {
    return Map.of(
      "initialize", this::handleInitialize,
      "ping", this::handlePing
    );
  }

  private Future<JsonResponse> handleInitialize(ServerRequest serverRequest, JsonRequest request) {
    // Build server capabilities from registered features
    ServerCapabilities capabilities = new ServerCapabilities();

    for (ServerFeature feature : getServer().features()) {
      Set<String> featureCapabilities = feature.getCapabilities();

      // Check for specific capability patterns and set them based on options
      if (featureCapabilities.stream().anyMatch(cap -> cap.startsWith("prompts/"))) {
        capabilities.setPrompts(new PromptsCapability().setListChanged(feature.getNotificationChannels().contains(PromptListChangedNotification.METHOD)));
      }
      if (featureCapabilities.stream().anyMatch(cap -> cap.startsWith("resources/"))) {
        ResourcesCapability resourcesCap = new ResourcesCapability();

        // Subscribe capability requires sessions
        if (getServer().getOptions().getStreamingEnabled()) {
          resourcesCap.setSubscribe(true);
        }

        resourcesCap.setListChanged(feature.getNotificationChannels().contains(ResourceListChangedNotification.METHOD));
        capabilities.setResources(resourcesCap);
      }
      if (featureCapabilities.stream().anyMatch(cap -> cap.startsWith("tools/"))) {
        capabilities.setTools(new ToolsCapability().setListChanged(feature.getNotificationChannels().contains(ToolListChangedNotification.METHOD)));
      }
      if (featureCapabilities.stream().anyMatch(cap -> cap.equals("logging"))) {
        capabilities.setLogging(new JsonObject());
      }
      if (featureCapabilities.stream().anyMatch(cap -> cap.startsWith("completion/"))) {
        capabilities.setCompletions(new JsonObject());
      }
    }

    InitializeResult result = new InitializeResult()
      .setServerInfo(new Implementation()
        .setName(getServer().getOptions().getServerName())
        .setVersion(getServer().getOptions().getServerVersion()))
      .setCapabilities(capabilities);

    return Future.succeededFuture(JsonResponse.success(request, result.toJson()));
  }

  private Future<JsonResponse> handlePing(ServerRequest serverRequest, JsonRequest request) {
    return Future.succeededFuture(JsonResponse.success(request, new JsonObject()));
  }
}
