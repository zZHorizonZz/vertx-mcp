package io.vertx.mcp.server.impl;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.Implementation;
import io.vertx.mcp.common.capabilities.PromptsCapability;
import io.vertx.mcp.common.capabilities.ResourcesCapability;
import io.vertx.mcp.common.capabilities.ServerCapabilities;
import io.vertx.mcp.common.capabilities.ToolsCapability;
import io.vertx.mcp.common.rpc.JsonError;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.common.result.InitializeResult;
import io.vertx.mcp.server.ModelContextProtocolServer;
import io.vertx.mcp.server.ServerFeature;
import io.vertx.mcp.server.ServerOptions;
import io.vertx.mcp.server.ServerRequest;

import java.util.Set;

/**
 * Handles core MCP protocol operations like initialize and ping.
 * This is a required feature that manages protocol-level communication.
 */
public class ProtocolServerFeature implements ServerFeature {

  private final ModelContextProtocolServer server;
  private final ServerOptions options;

  public ProtocolServerFeature(ModelContextProtocolServer server, ServerOptions options) {
    this.server = server;
    this.options = options;
  }

  @Override
  public void handle(ServerRequest serverRequest) {
    JsonRequest request = serverRequest.getJsonRequest();

    if (request == null) {
      serverRequest.response().end(
        new JsonResponse(JsonError.internalError("No JSON-RPC request found"), null)
      );
      return;
    }

    String method = request.getMethod();

    Future<JsonResponse> responseFuture;
    switch (method) {
      case "initialize":
        responseFuture = handleInitialize(request, serverRequest);
        break;
      case "ping":
        responseFuture = handlePing(request);
        break;
      default:
        responseFuture = Future.succeededFuture(
          JsonResponse.error(request, JsonError.methodNotFound(method))
        );
        break;
    }

    responseFuture.onComplete(ar -> {
      if (ar.succeeded()) {
        serverRequest.response().end(ar.result());
      } else {
        serverRequest.response().end(
          JsonResponse.error(request, JsonError.internalError(ar.cause().getMessage()))
        );
      }
    });
  }

  private Future<JsonResponse> handleInitialize(JsonRequest request, ServerRequest serverRequest) {
    // Build server capabilities from registered features
    ServerCapabilities capabilities = new ServerCapabilities();

    for (ServerFeature feature : server.features()) {
      Set<String> featureCapabilities = feature.getCapabilities();

      // Check for specific capability patterns and set them based on options
      if (featureCapabilities.stream().anyMatch(cap -> cap.startsWith("prompts/"))) {
        capabilities.setPrompts(new PromptsCapability().setListChanged(options.getNotificationsEnabled()));
      }
      if (featureCapabilities.stream().anyMatch(cap -> cap.startsWith("resources/"))) {
        ResourcesCapability resourcesCap = new ResourcesCapability();
        // Subscribe capability requires sessions
        if (options.getSessionsEnabled()) {
          resourcesCap.setSubscribe(true);
        }
        resourcesCap.setListChanged(options.getNotificationsEnabled());
        capabilities.setResources(resourcesCap);
      }
      if (featureCapabilities.stream().anyMatch(cap -> cap.startsWith("tools/"))) {
        capabilities.setTools(new ToolsCapability().setListChanged(options.getNotificationsEnabled()));
      }
      if (featureCapabilities.stream().anyMatch(cap -> cap.equals("logging"))) {
        capabilities.setLogging(new JsonObject());
      }
    }

    // Session creation is handled by HttpServerRequestImpl during request parsing

    InitializeResult result = new InitializeResult()
      .setServerInfo(new Implementation()
        .setName(options.getServerName())
        .setVersion(options.getServerVersion()))
      .setCapabilities(capabilities);

    return Future.succeededFuture(JsonResponse.success(request, result.toJson()));
  }

  private Future<JsonResponse> handlePing(JsonRequest request) {
    // Ping just returns an empty success response
    JsonObject result = new JsonObject();
    return Future.succeededFuture(JsonResponse.success(request, result));
  }

  @Override
  public Set<String> getCapabilities() {
    return Set.of("initialize", "ping");
  }
}
