package io.vertx.mcp.client.feature;

import io.vertx.core.Future;
import io.vertx.mcp.client.ClientResponse;
import io.vertx.mcp.client.ElicitationHandler;
import io.vertx.mcp.client.impl.ClientFeatureBase;
import io.vertx.mcp.common.request.ElicitRequest;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * The ElicitationClientFeature class implements the ClientFeatureBase and provides functionality to handle elicitation-related operations.
 * This feature allows the client to respond to elicitation/create requests from the server, enabling the server to sendRequest
 * structured user input from the client.
 *
 * @version 2025-06-18
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/server/elicitation">Client Features - Elicitation</a>
 */
public class ElicitationClientFeature extends ClientFeatureBase {

  private ElicitationHandler elicitationHandler;

  /**
   * Sets the handler for elicitation requests. This handler is called when the server requests elicitation from the client.
   *
   * @param handler the elicitation handler
   * @return this instance for method chaining
   */
  public ElicitationClientFeature setElicitationHandler(ElicitationHandler handler) {
    this.elicitationHandler = handler;
    return this;
  }

  @Override
  public Map<String, BiFunction<ClientResponse, JsonRequest, Future<JsonResponse>>> getHandlers() {
    return Map.of(
      "elicitation/create", this::handleElicit
    );
  }

  private Future<JsonResponse> handleElicit(ClientResponse clientResponse, JsonRequest request) {
    if (elicitationHandler == null) {
      return Future.failedFuture("No elicitation handler registered");
    }

    ElicitRequest elicitRequest = new ElicitRequest(request.getNamedParams());
    return elicitationHandler.apply(elicitRequest).map(result -> JsonResponse.success(request, result.toJson()));
  }
}
