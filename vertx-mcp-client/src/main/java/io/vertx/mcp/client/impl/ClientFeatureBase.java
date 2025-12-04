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

public abstract class ClientFeatureBase implements ClientFeature {

  public abstract Map<String, BiFunction<ClientResponse, JsonRequest, Future<JsonResponse>>> getHandlers();

  @Override
  public void handle(JsonRequest request) {
    clientResponse.handler(json -> {
      JsonRequest request = new JsonRequest(json);

      if (request.getMethod() == null) {
        return;
      }

      String method = request.getMethod();
      BiFunction<ClientResponse, JsonRequest, Future<JsonResponse>> handler = getHandlers().get(method);

      if (handler == null) {
        return;
      }

      handler.apply(clientResponse, request).onComplete(ar -> {
        if (ar.succeeded()) {
          // Send response back to server
          clientResponse.session().sendRequest(new Request(method, null) {
            @Override
            public JsonObject toJson() {
              return ar.result().toJson();
            }
          });
        } else {
          // Send error response back to server
          clientResponse.session().sendRequest(new Request(method, null) {
            @Override
            public JsonObject toJson() {
              return JsonResponse.error(request, JsonError.internalError(ar.cause().getMessage())).toJson();
            }
          });
        }
      });
    });
  }

  @Override
  public Set<String> getCapabilities() {
    return getHandlers().keySet();
  }
}
