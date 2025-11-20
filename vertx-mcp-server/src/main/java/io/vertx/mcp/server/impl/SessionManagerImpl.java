package io.vertx.mcp.server.impl;

import io.vertx.core.Vertx;
import io.vertx.mcp.common.capabilities.ClientCapabilities;
import io.vertx.mcp.server.ServerOptions;
import io.vertx.mcp.server.ServerSession;
import io.vertx.mcp.server.SessionManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManagerImpl implements SessionManager {

  private final Vertx vertx;
  private final ServerOptions options;
  private final Map<String, ServerSession> sessions = new ConcurrentHashMap<>();

  public SessionManagerImpl(Vertx vertx, ServerOptions options) {
    this.vertx = vertx;
    this.options = options;

    /*this.vertx.eventBus().consumer("io.vertx.mcp.server.notification", message -> {
      String id = message.headers().get("Mcp-Session-Id");
      String notification =
      ServerSession session = sessions.get(sessionId);
      if (session != null) {
        session.sendNotification()
        //session.notify(message.body());
      }
    });*/
  }

  @Override
  public ServerSession createSession(ClientCapabilities capabilities) {
    if (sessions.size() >= options.getMaxSessions()) {
      throw new IllegalStateException("Maximum session limit reached");
    }

    String sessionId = UUID.randomUUID().toString();
    ServerSession session = new ServerSessionImpl(sessionId, options.getStreamingEnabled(), capabilities);

    sessions.put(sessionId, session);

    // Set up timeout to clean up inactive sessions
    long timeoutMs = options.getSessionTimeoutMs();
    if (timeoutMs > 0) {
      vertx.setTimer(timeoutMs, timerId -> {
        ServerSession existingSession = sessions.get(sessionId);
        if (existingSession != null && !existingSession.isActive()) {
          sessions.remove(sessionId);
        }
      });
    }

    return session;
  }

  @Override
  public ServerSession getSession(String sessionId) {
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
