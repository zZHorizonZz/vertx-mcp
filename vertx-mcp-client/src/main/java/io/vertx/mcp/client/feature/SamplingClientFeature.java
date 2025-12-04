package io.vertx.mcp.client.feature;

import io.vertx.core.Future;
import io.vertx.mcp.client.ClientResponse;
import io.vertx.mcp.client.SamplingHandler;
import io.vertx.mcp.client.impl.ClientFeatureBase;
import io.vertx.mcp.common.request.CreateMessageRequest;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * The SamplingClientFeature class implements the ClientFeatureBase and provides functionality to handle sampling-related operations.
 * This feature allows the client to respond to sampling/createMessage requests from the server, enabling the server to sendRequest
 * LLM completions from the client.
 *
 * @version 2025-06-18
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/server/sampling">Client Features - Sampling</a>
 */
public class SamplingClientFeature extends ClientFeatureBase {

  private SamplingHandler samplingHandler;

  /**
   * Sets the handler for creating messages. This handler is called when the server requests message creation via LLM sampling.
   *
   * @param handler the sampling handler
   * @return this instance for method chaining
   */
  public SamplingClientFeature setSamplingHandler(SamplingHandler handler) {
    this.samplingHandler = handler;
    return this;
  }

  @Override
  public Map<String, BiFunction<ClientResponse, JsonRequest, Future<JsonResponse>>> getHandlers() {
    return Map.of(
      "sampling/createMessage", this::handleCreateMessage
    );
  }

  private Future<JsonResponse> handleCreateMessage(ClientResponse clientResponse, JsonRequest request) {
    if (samplingHandler == null) {
      return Future.failedFuture("No sampling handler registered");
    }

    CreateMessageRequest createMessageRequest = new CreateMessageRequest(request.getNamedParams());
    return samplingHandler.apply(createMessageRequest).map(result -> JsonResponse.success(request, result.toJson()));
  }
}
