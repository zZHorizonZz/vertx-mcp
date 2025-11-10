package io.vertx.mcp.server.impl;

import io.vertx.core.Vertx;
import io.vertx.mcp.server.ServerOptions;
import io.vertx.mcp.server.Session;
import io.vertx.mcp.server.SessionManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages HTTP sessions for MCP streamable HTTP transport. Sessions are created during initialize and tracked for subsequent requests.
 */
public class SessionManagerImpl implements SessionManager {

  private final Map<String, SessionImpl> sessions = new ConcurrentHashMap<>();
  private final ServerOptions options;
  private final Vertx vertx;

  public SessionManagerImpl(Vertx vertx, ServerOptions options) {
    this.vertx = vertx;
    this.options = options;
  }

  @Override
  public Session createSession() {
    if (sessions.size() >= options.getMaxSessions()) {
      throw new IllegalStateException("Maximum session limit reached");
    }

    String sessionId = UUID.randomUUID().toString();
    SessionImpl session = new SessionImpl(sessionId, options.getStreamingEnabled());
    sessions.put(sessionId, session);

    // Set up timeout to clean up inactive sessions
    long timeoutMs = options.getSessionTimeoutMs();
    if (timeoutMs > 0) {
      vertx.setTimer(timeoutMs, timerId -> {
        SessionImpl existingSession = sessions.get(sessionId);
        if (existingSession != null && !existingSession.isActive()) {
          sessions.remove(sessionId);
        }
      });
    }

    return session;
  }

  @Override
  public Session getSession(String sessionId) {
    return sessions.get(sessionId);
  }

  @Override
  public void removeSession(String sessionId) {
    sessions.remove(sessionId);
  }

  @Override
  public int getSessionCount() {
    return sessions.size();
  }
}
