package io.vertx.mcp.server;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.mcp.common.capabilities.ClientCapabilities;

/**
 * This interface defines a manager for handling sessions in the server. It provides methods for creating, retrieving, removing, and counting sessions. A session represents a
 * connection between a client and the server and can be used to manage communication, such as Server-Sent Events (SSE) or other types of streaming.
 */
@VertxGen
public interface SessionManager {
  /**
   * Create and register a new session.
   *
   * @return the created session
   */
  ServerSession createSession(ClientCapabilities capabilities);

  /**
   * Get an existing session by ID.
   *
   * @param sessionId the session ID
   * @return the session, or null if not found
   */
  ServerSession getSession(String sessionId);

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
