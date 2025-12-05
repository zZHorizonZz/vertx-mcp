package io.vertx.mcp.client;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.rpc.JsonRequest;

import java.util.Set;
import java.util.function.Function;

/**
 * The {@code ClientFeature} interface represents a client feature in a JSON-RPC framework. It serves as a handler for processing client responses and provides a mechanism to
 * define and retrieve the capabilities associated with the client feature.
 */
@VertxGen
public interface ClientFeature extends Function<JsonRequest, Future<JsonObject>> {

  /**
   * Initializes the client feature with the provided Vert.x instance. This method is used to set up the client feature and prepare it for handling responses within the Vert.x
   * context.
   *
   * @param vertx the Vert.x instance to be used for initialization
   */
  default void init(Vertx vertx) {
    // Default implementation does nothing
  }

  /**
   * Retrieves a set of capabilities associated with this client feature.
   *
   * @return a set of strings representing the capabilities provided by the client feature
   */
  Set<String> getCapabilities();

  /**
   * Checks if the given capability is supported.
   *
   * @param capability the name of the capability to check
   * @return true if the capability is supported, false otherwise
   */
  default boolean hasCapability(String capability) {
    return getCapabilities().contains(capability);
  }
}
