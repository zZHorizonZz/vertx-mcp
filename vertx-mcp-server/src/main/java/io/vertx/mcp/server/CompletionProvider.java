package io.vertx.mcp.server;

import io.vertx.core.Future;
import io.vertx.mcp.common.completion.Completion;
import io.vertx.mcp.common.completion.CompletionArgument;
import io.vertx.mcp.common.completion.CompletionContext;

import java.util.Set;

/**
 * Interface for server features that can provide completion suggestions. Features implementing this interface will be queried by the CompletionServerFeature when completion
 * requests are received for their supported reference types.
 *
 * @version 2025-06-18
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/server/utilities/completion">Server Utilities - Completion</a>
 */
public interface CompletionProvider {

  /**
   * Handles a completion request for a specific reference.
   *
   * @param refType the type of reference (e.g., "ref/prompt", "ref/resource")
   * @param refName the name or URI of the reference
   * @param argument the argument being completed
   * @param context the completion context with previously resolved arguments
   * @return a Future containing the completion suggestions
   */
  Future<Completion> handleCompletion(String refType, String refName, CompletionArgument argument, CompletionContext context);

  /**
   * Returns the set of reference types this provider supports.
   *
   * @return set of supported reference types (e.g., "ref/prompt", "ref/resource")
   */
  Set<String> getCompletionCapabilities();

}
