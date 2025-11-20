package io.vertx.mcp.server.impl;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.capabilities.ClientCapabilities;
import io.vertx.mcp.common.notification.*;
import io.vertx.mcp.common.rpc.JsonNotification;
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

  /**
   * Event bus address for sending notifications to sessions.
   */
  public static final String NOTIFICATION_ADDRESS = "io.vertx.mcp.server.notification";

  public SessionManagerImpl(Vertx vertx, ServerOptions options) {
    this.vertx = vertx;
    this.options = options;

    // Register event bus consumer for notifications
    this.vertx.eventBus().<JsonObject> consumer(NOTIFICATION_ADDRESS, message -> {
      String sessionId = message.headers().get("Mcp-Session-Id");

      JsonNotification notification = new JsonNotification(message.body());
      String method = new JsonNotification(message.body()).getMethod();
      JsonObject params = notification.getParams() != null ? (JsonObject) notification.getParams() : new JsonObject();

      switch (method) {
        case "notifications/resources/updated": {
          if (sessionId != null && params != null) {
            ServerSession session = sessions.get(sessionId);
            if (session != null && session.isStreaming()) {
              session.sendNotification(new ResourceUpdatedNotification(params));
            }
          }
          break;
        }
        case "notifications/resources/list_changed":
          sendNotification(new ResourceListChangedNotification(params));
          break;
        case "notifications/tools/list_changed":
          sendNotification(new ToolListChangedNotification(params));
          break;
        case "notifications/prompts/list_changed":
          sendNotification(new PromptListChangedNotification(params));
          break;
        default:
          // Unknown notification type, ignore
          break;
      }
    });
  }

  private void sendNotification(Notification notification) {
    for (ServerSession session : sessions.values()) {
      if (session.isStreaming()) {
        session.sendNotification(notification);
      }
    }
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
