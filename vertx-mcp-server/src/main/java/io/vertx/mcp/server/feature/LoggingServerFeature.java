package io.vertx.mcp.server.feature;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.request.SetLevelRequest;
import io.vertx.mcp.common.rpc.JsonError;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.server.ServerRequest;
import io.vertx.mcp.server.ServerSession;
import io.vertx.mcp.server.impl.ServerFeatureBase;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * The LoggingServerFeature class implements support for MCP logging capabilities. It allows clients to set their desired log level and servers to send log messages as
 * notifications.
 *
 * @version 2025-06-18
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/server/utilities/logging">Server Utilities - Logging</a>
 */
public class LoggingServerFeature extends ServerFeatureBase {

  @Override
  public Map<String, BiFunction<ServerRequest, JsonRequest, Future<JsonResponse>>> getHandlers() {
    return Map.of("logging/setLevel", this::handleSetLevel);
  }

  private Future<JsonResponse> handleSetLevel(ServerRequest serverRequest, JsonRequest request) {
    JsonObject params = request.getNamedParams();
    if (params == null) {
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.invalidParams("Missing parameters"))
      );
    }

    SetLevelRequest setLevelRequest = new SetLevelRequest(params);

    if (setLevelRequest.getLevel() == null) {
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.invalidParams("Missing 'level' parameter"))
      );
    }

    // Require a session for logging
    if (serverRequest.session() == null) {
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.invalidRequest("Logging requires a session-scoped request"))
      );
    }

    ServerSession session = serverRequest.session();

    session.setLoggingLevel(setLevelRequest.getLevel());

    return Future.succeededFuture(JsonResponse.success(request, new JsonObject()));
  }
}
