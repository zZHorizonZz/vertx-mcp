package io.vertx.mcp.client;

import io.vertx.core.Future;
import io.vertx.mcp.common.capabilities.ClientCapabilities;

public interface ClientTransport {
  Future<ClientSession> connect(ClientCapabilities capabilities);

  Future<ClientResponse> request();
}
