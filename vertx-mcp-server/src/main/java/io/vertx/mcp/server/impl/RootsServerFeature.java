package io.vertx.mcp.server.impl;

import io.vertx.core.Future;
import io.vertx.mcp.common.result.ListRootsResult;
import io.vertx.mcp.common.root.Root;
import io.vertx.mcp.common.rpc.JsonError;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.server.RootsHandler;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * The RootsServerFeature class implements the ServerFeatureBase and provides functionality to handle JSON-RPC requests related to roots management. This includes listing
 * available roots and registering root handlers.
 *
 * @version 2025-06-18
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/server/roots">Server Features - Roots</a>
 */
public class RootsServerFeature extends ServerFeatureBase {

  private RootsHandler rootsHandler;

  @Override
  public Map<String, Function<JsonRequest, Future<JsonResponse>>> getHandlers() {
    return Map.of(
      "roots/list", this::handleListRoots
    );
  }

  private Future<JsonResponse> handleListRoots(JsonRequest request) {
    if (rootsHandler == null) {
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.internalError("No roots handler registered"))
      );
    }

    return rootsHandler.listRoots()
      .compose(roots -> {
        ListRootsResult result = new ListRootsResult().setRoots(roots);
        return Future.succeededFuture(JsonResponse.success(request, result.toJson()));
      })
      .recover(err -> Future.succeededFuture(
        JsonResponse.error(request, JsonError.internalError(err.getMessage()))
      ));
  }

  /**
   * Sets the roots handler for this feature.
   *
   * @param handler the roots handler to register
   */
  public void setRootsHandler(RootsHandler handler) {
    this.rootsHandler = handler;
  }

  /**
   * Gets the current roots handler.
   *
   * @return the registered roots handler, or null if none is registered
   */
  public RootsHandler getRootsHandler() {
    return rootsHandler;
  }

  /**
   * Checks if a roots handler is registered.
   *
   * @return true if a handler is registered, false otherwise
   */
  public boolean hasRootsHandler() {
    return rootsHandler != null;
  }
}
