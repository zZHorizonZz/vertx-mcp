package io.vertx.mcp.server.impl;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.capabilities.ClientCapabilities;
import io.vertx.mcp.common.notification.Notification;
import io.vertx.mcp.common.request.PingRequest;
import io.vertx.mcp.common.rpc.JsonCodec;
import io.vertx.mcp.common.rpc.JsonNotification;
import io.vertx.mcp.server.ServerNotification;
import io.vertx.mcp.server.ServerOptions;
import io.vertx.mcp.server.ServerSession;
import io.vertx.mcp.server.SessionManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class SessionManagerImpl implements SessionManager {

  private final Vertx vertx;
  private final ServerOptions options;
  private final Map<String, Long> sessionLastPing = new ConcurrentHashMap<>();
  private final Map<String, ServerSession> sessions = new ConcurrentHashMap<>();

  public SessionManagerImpl(Vertx vertx, ServerOptions options) {
    this.vertx = vertx;
    this.options = options;

    // Register event bus consumer for notifications
    this.vertx.eventBus().<JsonObject> consumer(NOTIFICATION_ADDRESS, message -> {
      String sessionId = message.headers().get("Mcp-Session-Id");

      ServerNotification notification = new ServerNotification(message.body());
      if (notification.getNotification() == null) {
        return;
      }

      JsonNotification jsonNotification = new JsonNotification(notification.getNotification());
      Notification decodedNotification = JsonCodec.decodeNotification(jsonNotification.getMethod(), jsonNotification.getNamedParams());

      if (!notification.isBroadcast() && sessionId != null) {
        sessions.values().stream().filter(session -> session.id().equals(sessionId)).forEach(session -> session.sendNotification(decodedNotification));
        return;
      } else if (!notification.isBroadcast()) {
        throw new IllegalStateException("Notification must be sent to a specific session");
      }

      sendNotification(decodedNotification);
    });

    this.vertx.setPeriodic(1000, (timerId) -> {
      for (Map.Entry<String, ServerSession> entry : sessions.entrySet()) {
        String sessionId = entry.getKey();
        ServerSession session = entry.getValue();
        long lastPing = sessionLastPing.getOrDefault(sessionId, 0L);

        if (System.currentTimeMillis() - lastPing > 5000) {
          sessionLastPing.put(sessionId, System.currentTimeMillis());
          session.sendRequest(new PingRequest());
        }

        /*if (System.currentTimeMillis() - lastPing > options.getSessionTimeoutMs()) {
          sessions.remove(sessionId);
          sessionLastPing.remove(sessionId);
        }*/
      }
    });
  }

  private Future<Void> sendNotification(Notification notification) {
    return Future.join(sessions.values().stream().map(session -> session.sendNotification(notification)).collect(Collectors.toList())).mapEmpty();
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
          //existingSession.close();
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
    sessionLastPing.remove(sessionId);
  }

  @Override
  public int getSessionCount() {
    return sessions.size();
  }
}
