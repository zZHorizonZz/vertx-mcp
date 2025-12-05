package io.vertx.mcp.client.feature;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.client.RootsHandler;
import io.vertx.mcp.client.impl.ClientFeatureBase;
import io.vertx.mcp.common.result.ListRootsResult;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;

import java.util.Map;
import java.util.function.Function;

/**
 * The RootsClientFeature class implements the ClientFeatureBase and provides functionality to handle roots-related operations. This feature allows the client to respond to
 * roots/list requests from the server.
 *
 * @version 2025-06-18
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/server/roots">Client Features - Roots</a>
 */
public class RootsClientFeature extends ClientFeatureBase {

  private RootsHandler rootsHandler;

  /**
   * Sets the handler for listing roots. This handler is called when the server requests the list of roots.
   *
   * @param handler the roots handler
   * @return this instance for method chaining
   */
  public RootsClientFeature setRootsHandler(RootsHandler handler) {
    this.rootsHandler = handler;
    return this;
  }

  @Override
  public Map<String, Function<JsonRequest, Future<JsonObject>>> getHandlers() {
    return Map.of(
      "roots/list", this::handleListRoots
    );
  }

  private Future<JsonObject> handleListRoots(JsonRequest request) {
    if (rootsHandler == null) {
      return Future.succeededFuture(JsonResponse.success(request, new ListRootsResult().toJson()).toJson());
    }

    return rootsHandler.get().map(result -> JsonResponse.success(request, result.toJson()).toJson());
  }
}
