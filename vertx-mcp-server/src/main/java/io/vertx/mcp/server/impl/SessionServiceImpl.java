package io.vertx.mcp.server.impl;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.server.SessionManager;
import io.vertx.mcp.server.SessionService;

public class SessionServiceImpl implements SessionService {

  private final Vertx vertx;
  private final SessionManager sessionManager;

  public SessionServiceImpl(Vertx vertx, SessionManager sessionManager) {
    this.vertx = vertx;
    this.sessionManager = sessionManager;
  }

  @Override
  public Future<Void> send(String sessionId, JsonObject message) {
    return null;
  }

  @Override
  public Future<Void> sendNotification(String sessionId, JsonObject message) {
    return null;
  }
}
