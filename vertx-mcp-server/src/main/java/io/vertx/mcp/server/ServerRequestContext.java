package io.vertx.mcp.server;

import io.vertx.mcp.common.request.Request;

/**
 * Context for handling server requests. Contains information about the current request and session.
 */
public interface ServerRequestContext {

  /**
   * Gets the current server session.
   *
   * @return server session
   */
  ServerSession session();

  /**
   * Gets the current request.
   *
   * @return request
   */
  Request request();

  /**
   * Gets the request ID if present.
   *
   * @return request ID, or null if not present
   */
  Object requestId();
}
