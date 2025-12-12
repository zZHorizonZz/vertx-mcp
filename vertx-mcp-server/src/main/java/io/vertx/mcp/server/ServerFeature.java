package io.vertx.mcp.server;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import java.util.Set;

/**
 * The {@code ServerFeature} interface represents a server feature in a JSON-RPC framework. It serves as a handler for processing server requests and provides a mechanism to define
 * and retrieve the capabilities associated with the server feature.
 */
@VertxGen
public interface ServerFeature extends Handler<ServerRequest> {

  /**
   * Initializes the server feature with the provided Vert.x instance. This method is used to set up the server feature and prepare it for handling requests within the Vert.x
   * context.
   *
   * @param vertx the Vert.x instance to be used for initialization
   */
  default void init(ModelContextProtocolServer server, Vertx vertx) {
    // Default implementation does nothing
  }

  /**
   * Retrieves a set of notification channels associated with the server feature. Notification channels represent distinct communication paths or topics that the server feature can
   * use to notify clients about specific events or updates.
   *
   * @return a set of strings where each string represents a unique notification channel
   */
  default Set<String> getNotificationChannels() {
    return Set.of();
  }

  /**
   * Retrieves a set of capabilities associated with this server feature.
   *
   * @return a set of strings representing the capabilities provided by the server feature
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
