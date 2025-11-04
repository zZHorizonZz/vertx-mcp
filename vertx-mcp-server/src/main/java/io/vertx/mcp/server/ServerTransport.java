package io.vertx.mcp.server;

import io.vertx.core.Handler;
import io.vertx.mcp.common.transport.Transport;

/**
 * Represents a server-side transport layer for MCP communication.
 * Extends the base Transport interface with server-specific functionality.
 */
public interface ServerTransport extends Transport {

  @Override
  ServerTransport messageHandler(Handler<io.vertx.core.json.JsonObject> handler);

  @Override
  ServerTransport closeHandler(Handler<Void> handler);

  @Override
  ServerTransport exceptionHandler(Handler<Throwable> handler);
}
