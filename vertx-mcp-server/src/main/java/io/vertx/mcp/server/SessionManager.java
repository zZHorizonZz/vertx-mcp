package io.vertx.mcp.server;

import io.vertx.codegen.annotations.VertxGen;

@VertxGen
public interface SessionManager {
  /**
   * Create and register a new session.
   *
   * @return the created session
   */
  Session createSession();

  /**
   * Get an existing session by ID.
   *
   * @param sessionId the session ID
   * @return the session, or null if not found
   */
  Session getSession(String sessionId);

  /**
   * Remove a session.
   *
   * @param sessionId the session ID
   */
  void removeSession(String sessionId);

  /**
   * Get the current session count.
   *
   * @return the number of active sessions
   */
  int getSessionCount();
}
