package io.vertx.mcp.client.impl;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.client.ClientFeature;
import io.vertx.mcp.client.ClientResponse;
import io.vertx.mcp.client.ModelContextProtocolClient;
import io.vertx.mcp.common.request.Request;
import io.vertx.mcp.common.rpc.JsonError;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;

import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class ClientFeatureBase implements ClientFeature {

  public abstract Map<String, Function<JsonRequest, Future<JsonObject>>> getHandlers();

  @Override
  public Future<JsonObject> apply(JsonRequest jsonRequest) {
    String method = jsonRequest.getMethod();
    Function<JsonRequest, Future<JsonObject>> handler = getHandlers().get(method);

    if (handler == null) {
      return Future.failedFuture(new IllegalArgumentException("No handler found for method: " + method));
    }

    return handler.apply(jsonRequest);
  }

  @Override
  public Set<String> getCapabilities() {
    return getHandlers().keySet();
  }
}
