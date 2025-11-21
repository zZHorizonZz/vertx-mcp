package io.vertx.mcp.server.impl;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.capabilities.ClientCapabilities;
import io.vertx.mcp.common.notification.*;
import io.vertx.mcp.common.request.PingRequest;
import io.vertx.mcp.common.rpc.JsonNotification;
import io.vertx.mcp.server.ServerOptions;
import io.vertx.mcp.server.ServerSession;
import io.vertx.mcp.server.SessionManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManagerImpl implements SessionManager {

  /**
   * Event bus address for sending notifications to sessions.
   */
  public static final String NOTIFICATION_ADDRESS = "io.vertx.mcp.server.notification";

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
          sendNotification(new ResourceListChangedNotification(params)).onComplete(ar -> {
            if (ar.succeeded()) {
              System.out.println("Resource list changed notification sent");
            } else {
              System.out.println("Resource list changed notification failed: " + ar.cause().getMessage());
            }
          });
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

    this.vertx.setPeriodic(1000, (timerId) -> {
      for (Map.Entry<String, ServerSession> entry : sessions.entrySet()) {
        String sessionId = entry.getKey();
        ServerSession session = entry.getValue();
        long lastPing = sessionLastPing.getOrDefault(sessionId, 0L);

        if (System.currentTimeMillis() - lastPing > 5000) {
          sessionLastPing.put(sessionId, System.currentTimeMillis());
          session.sendRequest(new PingRequest()).onComplete(ar -> {
            if (ar.succeeded()) {
              System.out.println("Ping response received");
            } else {
              System.out.println("Ping response failed: " + ar.cause().getMessage());
            }
          });
        }

        /*if (System.currentTimeMillis() - lastPing > options.getSessionTimeoutMs()) {
          sessions.remove(sessionId);
          sessionLastPing.remove(sessionId);
        }*/
      }
    });
  }

  private Future<Void> sendNotification(Notification notification) {
    return Future.join(sessions.values().stream().map(session -> session.sendNotification(notification)).toArray(Future[]::new)).mapEmpty();
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
  }

  @Override
  public int getSessionCount() {
    return sessions.size();
  }
}
