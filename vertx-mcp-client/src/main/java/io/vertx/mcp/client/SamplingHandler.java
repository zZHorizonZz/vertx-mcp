package io.vertx.mcp.client;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.mcp.common.request.CreateMessageRequest;
import io.vertx.mcp.common.result.CreateMessageResult;

import java.util.function.Function;

/**
 * Represents a handler responsible for processing sampling requests.
 * A sampling handler processes LLM message creation requests from the server.
 *
 * @version 2025-06-18
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/server/sampling">Client Features - Sampling</a>
 */
@VertxGen
public interface SamplingHandler extends ClientFeatureHandler<CreateMessageRequest, Future<CreateMessageResult>> {

  /**
   * Creates a new instance of a {@code SamplingHandler} with the specified function.
   *
   * @param name the name of the sampling handler
   * @param function the function that processes the create message request and returns a future result
   * @return a new {@code SamplingHandler} instance initialized with the provided parameters
   */
  @GenIgnore
  static SamplingHandler create(String name, Function<CreateMessageRequest, Future<CreateMessageResult>> function) {
    return new SamplingHandler() {
      @Override
      public String name() {
        return name;
      }

      @Override
      public Future<CreateMessageResult> apply(CreateMessageRequest request) {
        return function.apply(request);
      }
    };
  }
}
