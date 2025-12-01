package io.vertx.mcp.server.impl;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.mcp.common.notification.Notification;
import io.vertx.mcp.common.rpc.JsonError;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.server.ServerFeature;
import io.vertx.mcp.server.ServerRequest;
import io.vertx.mcp.server.SessionManager;

import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

public abstract class ServerFeatureBase implements ServerFeature {

  public abstract Map<String, BiFunction<ServerRequest, JsonRequest, Future<JsonResponse>>> getHandlers();

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
    BiFunction<ServerRequest, JsonRequest, Future<JsonResponse>> handler = getHandlers().get(method);

    if (handler == null) {
      serverRequest.response().end(
        JsonResponse.error(request, JsonError.methodNotFound(method))
      );
      return;
    }

    handler.apply(serverRequest, request).onComplete(ar -> {
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

  /**
   * Sends a notification to all sessions.
   *
   * @param vertx the Vertx instance used for interacting with the event bus
   * @param notification the notification object containing the data to be sent
   */
  protected void sendNotification(Vertx vertx, Notification notification) {
    if (vertx == null || notification == null) {
      return;
    }

    vertx.eventBus().send(SessionManager.NOTIFICATION_ADDRESS, notification.toNotification().toJson());
  }
}
