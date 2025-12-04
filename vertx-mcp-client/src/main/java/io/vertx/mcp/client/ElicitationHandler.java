package io.vertx.mcp.client;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.mcp.common.request.ElicitRequest;
import io.vertx.mcp.common.result.ElicitResult;

import java.util.function.Function;

/**
 * Represents a handler responsible for processing elicitation requests.
 * An elicitation handler processes requests for structured user input from the server.
 *
 * @version 2025-06-18
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/server/elicitation">Client Features - Elicitation</a>
 */
@VertxGen
public interface ElicitationHandler extends ClientFeatureHandler<ElicitRequest, Future<ElicitResult>> {

  /**
   * Creates a new instance of an {@code ElicitationHandler} with the specified function.
   *
   * @param name the name of the elicitation handler
   * @param function the function that processes the elicit sendRequest and returns a future result
   * @return a new {@code ElicitationHandler} instance initialized with the provided parameters
   */
  @GenIgnore
  static ElicitationHandler create(String name, Function<ElicitRequest, Future<ElicitResult>> function) {
    return new ElicitationHandler() {
      @Override
      public String name() {
        return name;
      }

      @Override
      public Future<ElicitResult> apply(ElicitRequest request) {
        return function.apply(request);
      }
    };
  }
}
