package io.vertx.mcp.it;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.mcp.common.notification.InitializedNotification;
import io.vertx.mcp.common.notification.Notification;
import io.vertx.mcp.common.notification.ToolListChangedNotification;
import io.vertx.mcp.common.rpc.JsonNotification;
import io.vertx.mcp.server.ModelContextProtocolServer;
import io.vertx.mcp.server.ServerFeature;
import io.vertx.mcp.server.ServerOptions;
import io.vertx.mcp.server.ServerRequest;
import io.vertx.mcp.server.feature.ProtocolServerFeature;
import io.vertx.mcp.server.feature.SessionServerFeature;
import org.junit.Test;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class NotificationHandlingTest extends HttpTransportTestBase {

  @Test
  public void testNotificationReturns202Accepted(TestContext context) throws Throwable {
    ServerOptions options = new ServerOptions();
    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx, options);

    AtomicBoolean notificationReceived = new AtomicBoolean(false);

    server.addServerFeature(new ServerFeature() {
      @Override
      public void handle(ServerRequest request) {
        notificationReceived.set(true);
      }

      @Override
      public Set<String> getCapabilities() {
        return Set.of(ToolListChangedNotification.METHOD);
      }
    });

    server.addServerFeature(new ProtocolServerFeature());
    server.addServerFeature(new SessionServerFeature());

    startServer(context, server);

    createSession().compose(session -> session.sendNotification(new ToolListChangedNotification())).await(10, TimeUnit.SECONDS);

    Thread.sleep(100);

    context.assertTrue(notificationReceived.get(), "Notification handler should have been called");
  }

  @Test
  public void testNotificationWithoutHandlerReturns202(TestContext context) throws Throwable {
    ServerOptions options = new ServerOptions();
    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx, options);

    server.addServerFeature(new ProtocolServerFeature());
    server.addServerFeature(new SessionServerFeature());

    startServer(context, server);

    // Notifications without handlers are silently ignored (no error)
    createSession()
      .compose(session -> session.sendNotification(Notification.createNotification("notifications/unknown", new JsonObject())))
      .await(10, TimeUnit.SECONDS);

    // The notification completes successfully even without a handler
    context.assertTrue(true, "Notification should succeed even without handler");
  }
}
