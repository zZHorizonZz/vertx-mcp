package io.vertx.mcp.server.impl;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.rpc.JsonError;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.server.ServerFeature;
import io.vertx.mcp.server.ServerRequest;

import java.util.Set;

public class ToolServerFeature implements ServerFeature {

  @Override
  public void handle(ServerRequest serverRequest) {
    // Retrieve the parsed JSON-RPC request from the ServerRequest
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
      case "tools/list":
        responseFuture = handleListTools(request);
        break;
      case "tools/call":
        responseFuture = handleCallTool(request);
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

  private Future<JsonResponse> handleListTools(JsonRequest request) {
    // TODO: Implement tool listing
    JsonObject result = new JsonObject().put("tools", new JsonArray());
    return Future.succeededFuture(JsonResponse.success(request, result));
  }

  private Future<JsonResponse> handleCallTool(JsonRequest request) {
    // TODO: Implement tool calling
    return Future.succeededFuture(
      JsonResponse.error(request, JsonError.internalError("Tool calling not yet implemented"))
    );
  }

  @Override
  public Set<String> getCapabilities() {
    return Set.of("tools/list", "tools/call");
  }
}
