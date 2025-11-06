package io.vertx.mcp.server;

import io.vertx.core.Handler;

import java.util.Set;

/**
 * Base marker interface for MCP server features. Server features handle specific capabilities like tools, resources, prompts, etc. Context is obtained from Vert.x context.
 */
public interface ServerFeature extends Handler<ServerRequest> {

  Set<String> getCapabilities();

  default boolean hasCapability(String capability) {
    return getCapabilities().contains(capability);
  }

}
