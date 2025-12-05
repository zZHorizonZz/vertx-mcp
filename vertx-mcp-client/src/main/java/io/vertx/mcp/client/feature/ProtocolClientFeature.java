package io.vertx.mcp.client.feature;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.client.impl.ClientFeatureBase;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;

import java.util.Map;
import java.util.function.Function;

/**
 * The ProtocolClientFeature class implements the ClientFeatureBase and provides functionality to handle core MCP protocol operations like ping. This is a required
 * feature that manages protocol-level communication from the server.
 *
 * @version 2025-06-18
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/basic/lifecycle">Client Features - Lifecycle</a>
 */
public class ProtocolClientFeature extends ClientFeatureBase {

  @Override
  public Map<String, Function<JsonRequest, Future<JsonObject>>> getHandlers() {
    return Map.of(
      "ping", this::handlePing
    );
  }

  private Future<JsonObject> handlePing(JsonRequest request) {
    return Future.succeededFuture(JsonResponse.success(request, new JsonObject()).toJson());
  }
}
