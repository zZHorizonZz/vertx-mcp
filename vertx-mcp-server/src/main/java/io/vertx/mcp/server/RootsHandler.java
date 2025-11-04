package io.vertx.mcp.server;

import io.vertx.core.Future;
import io.vertx.mcp.common.root.Root;

import java.util.List;

/**
 * Handler for root operations like listing roots.
 */
public interface RootsHandler {

  /**
   * Lists all available roots.
   *
   * @return a future that completes with the list of roots
   */
  Future<List<Root>> listRoots();
}
