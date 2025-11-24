package io.vertx.mcp.common.transport;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

/**
 * Represents a transport layer for MCP communication. Handles the low-level sending and receiving of JSON-RPC messages.
 */
@VertxGen
public interface Transport {
  /**
   * Sends a JSON-RPC message.
   *
   * @param message the JSON-RPC message to send
   * @return a future that completes when the message is sent
   */
  Future<Void> send(JsonObject message);

  /**
   * Sets the handler to be called when a message is received.
   *
   * @param handler the message handler
   * @return this transport for method chaining
   */
  Transport messageHandler(Handler<JsonObject> handler);

  /**
   * Sets the handler to be called when the transport connection is closed.
   *
   * @param handler the close handler
   * @return this transport for method chaining
   */
  Transport closeHandler(Handler<Void> handler);

  /**
   * Sets the handler to be called when an error occurs.
   *
   * @param handler the error handler
   * @return this transport for method chaining
   */
  Transport exceptionHandler(Handler<Throwable> handler);

  /**
   * Checks if the transport is currently connected.
   *
   * @return true if connected, false otherwise
   */
  boolean isConnected();

  /**
   * Closes the transport connection.
   *
   * @return a future that completes when the transport is closed
   */
  Future<Void> close();
}
