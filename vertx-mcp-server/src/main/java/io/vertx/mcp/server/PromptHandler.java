package io.vertx.mcp.server;

import io.vertx.core.Future;
import io.vertx.mcp.common.prompt.Prompt;

import java.util.List;

/**
 * Handler for prompt operations like listing prompts.
 */
public interface PromptHandler extends ServerFeature {

  /**
   * Lists all available prompts.
   *
   * @param cursor pagination cursor
   * @return a future that completes with the list of prompts
   */
  Future<List<Prompt>> listPrompts(String cursor);
}
