package io.vertx.mcp.client.feature;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.client.impl.ClientFeatureBase;
import io.vertx.mcp.common.result.ListRootsResult;
import io.vertx.mcp.common.root.Root;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The RootsClientFeature class implements the ClientFeatureBase and provides functionality to handle roots-related operations. This feature allows the client to respond to
 * roots/list requests from the server.
 *
 * @version 2025-06-18
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/server/roots">Client Features - Roots</a>
 */
public class RootsClientFeature extends ClientFeatureBase {

  private final Map<String, Root> roots = new HashMap<>();

  @Override
  public Map<String, Function<JsonRequest, Future<JsonObject>>> getHandlers() {
    return Map.of(
      "roots/list", this::handleListRoots
    );
  }

  private Future<JsonObject> handleListRoots(JsonRequest request) {
    //TODO Handle pagination
    ListRootsResult result = new ListRootsResult();
    result.setRoots(roots.values().stream().collect(Collectors.toUnmodifiableList()));
    return Future.succeededFuture(JsonResponse.success(request, result.toJson()).toJson());
  }

  public RootsClientFeature addRoot(Root root) {
    roots.put(root.getName(), root);
    return this;
  }

  public RootsClientFeature removeRoot(String name) {
    roots.remove(name);
    return this;
  }

  public Map<String, Root> roots() {
    return roots;
  }
}
