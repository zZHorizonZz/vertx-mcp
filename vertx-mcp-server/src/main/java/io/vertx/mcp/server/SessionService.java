package io.vertx.mcp.server;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.server.impl.SessionServiceImpl;

@VertxGen
@ProxyGen
public interface SessionService {

  static SessionService create(Vertx vertx, SessionManager sessionManager) {
    return new SessionServiceImpl(vertx, sessionManager);
  }

  static SessionService createProxy(Vertx vertx, String address) {
    //return new SessionServiceEBProxy(vertx, address);
    return null;
  }

  Future<Void> send(String sessionId, JsonObject message);

  Future<Void> sendNotification(String sessionId, JsonObject message);
}
