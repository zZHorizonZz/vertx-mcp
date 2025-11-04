package io.vertx.mcp.server;

import io.vertx.core.Future;
import io.vertx.mcp.common.tool.Tool;

import java.util.List;

/**
 * Handler for tool operations like listing and calling tools.
 */
public interface ToolHandler {

  /**
   * Lists all available tools.
   *
   * @param cursor pagination cursor
   * @return a future that completes with the list of tools
   */
  Future<List<Tool>> listTools(String cursor);
}
