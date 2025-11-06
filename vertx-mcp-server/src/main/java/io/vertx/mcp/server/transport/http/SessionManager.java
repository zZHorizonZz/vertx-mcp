package io.vertx.mcp.server.transport.http;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.mcp.server.ServerOptions;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages HTTP sessions for MCP streamable HTTP transport.
 * Sessions are created during initialize and tracked for subsequent requests.
 */
public class SessionManager {

  private final Map<String, HttpSession> sessions = new ConcurrentHashMap<>();
  private final ServerOptions options;
  private final Vertx vertx;

  public SessionManager(Vertx vertx, ServerOptions options) {
    this.vertx = vertx;
    this.options = options;
  }

  /**
   * Generate a new session ID.
   *
   * @return a unique session ID
   */
  public String generateSessionId() {
    return UUID.randomUUID().toString();
  }

  /**
   * Create and register a new session.
   *
   * @param sessionId the session ID
   * @param httpResponse the HTTP response for this session
   * @return the created session
   */
  public HttpSession createSession(String sessionId, HttpServerResponse httpResponse) {
    if (sessions.size() >= options.getMaxSessions()) {
      throw new IllegalStateException("Maximum session limit reached");
    }

    HttpSession session = new HttpSession(sessionId, httpResponse);
    sessions.put(sessionId, session);

    // Set up timeout to clean up inactive sessions
    long timeoutMs = options.getSessionTimeoutMs();
    if (timeoutMs > 0) {
      vertx.setTimer(timeoutMs, timerId -> {
        HttpSession existingSession = sessions.get(sessionId);
        if (existingSession != null && !existingSession.isActive()) {
          sessions.remove(sessionId);
        }
      });
    }

    return session;
  }

  /**
   * Get an existing session by ID.
   *
   * @param sessionId the session ID
   * @return the session, or null if not found
   */
  public HttpSession getSession(String sessionId) {
    return sessions.get(sessionId);
  }

  /**
   * Remove a session.
   *
   * @param sessionId the session ID
   */
  public void removeSession(String sessionId) {
    sessions.remove(sessionId);
  }

  /**
   * Get the current session count.
   *
   * @return the number of active sessions
   */
  public int getSessionCount() {
    return sessions.size();
  }
}
