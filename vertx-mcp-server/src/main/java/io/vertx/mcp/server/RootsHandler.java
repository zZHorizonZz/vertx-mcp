package io.vertx.mcp.server;

import io.vertx.core.Future;
import io.vertx.mcp.common.root.Root;

import java.util.List;

/**
 * Represents a handler responsible for managing roots operations. A roots handler provides access to the file system roots
 * available to the server, enabling clients to understand the scope of accessible paths.
 *
 * @version 2025-06-18
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/server/roots">Server Features - Roots</a>
 */
public interface RootsHandler {

  /**
   * Lists all available roots.
   *
   * @return a future that completes with the list of roots
   */
  Future<List<Root>> listRoots();
}
