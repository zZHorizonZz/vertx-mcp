package io.vertx.mcp.server.impl;

import io.vertx.core.Future;
import io.vertx.mcp.common.rpc.JsonError;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.server.ServerFeature;
import io.vertx.mcp.server.ServerRequest;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public abstract class ServerFeatureBase implements ServerFeature {

  public abstract Map<String, Function<JsonRequest, Future<JsonResponse>>> getHandlers();

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
    Function<JsonRequest, Future<JsonResponse>> handler = getHandlers().get(method);

    if (handler == null) {
      serverRequest.response().end(
        JsonResponse.error(request, JsonError.methodNotFound(method))
      );
      return;
    }

    handler.apply(request).onComplete(ar -> {
      if (ar.succeeded()) {
        serverRequest.response().end(ar.result());
      } else {
        serverRequest.response().end(
          JsonResponse.error(request, JsonError.internalError(ar.cause().getMessage()))
        );
      }
    });
  }

  @Override
  public Set<String> getCapabilities() {
    return getHandlers().keySet();
  }
}
