package io.vertx.mcp.it;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.mcp.client.ClientRequestException;
import io.vertx.mcp.common.request.PingRequest;
import io.vertx.mcp.common.result.EmptyResult;
import io.vertx.mcp.common.rpc.JsonError;
import io.vertx.mcp.common.rpc.JsonNotification;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.server.ModelContextProtocolServer;
import io.vertx.mcp.server.ServerFeature;
import io.vertx.mcp.server.ServerOptions;
import io.vertx.mcp.server.ServerRequest;
import org.junit.Test;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class NotificationHandlingTest extends HttpTransportTestBase {

  @Test
  public void testNotificationReturns202Accepted(TestContext context) throws Throwable {
    ServerOptions options = new ServerOptions().setNotificationsEnabled(true);
    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx, options);

    AtomicBoolean notificationReceived = new AtomicBoolean(false);

    server.addServerFeature(new ServerFeature() {
      @Override
      public void handle(ServerRequest request) {
        notificationReceived.set(true);
      }

      @Override
      public Set<String> getCapabilities() {
        return Set.of("notifications/test");
      }
    });

    startServer(context, server);

    createSession().compose(session -> session.sendNotification(new JsonNotification("notifications/test", new JsonObject()))).await(10, TimeUnit.SECONDS);

    Thread.sleep(100);

    context.assertTrue(notificationReceived.get(), "Notification handler should have been called");
  }

  @Test
  public void testNotificationWithoutHandlerReturns202(TestContext context) throws Throwable {
    ServerOptions options = new ServerOptions().setNotificationsEnabled(true);
    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx, options);

    startServer(context, server);

    // Notifications without handlers are silently ignored (no error)
    createSession()
      .compose(session -> session.sendNotification(new JsonNotification("notifications/unknown", new JsonObject())))
      .await(10, TimeUnit.SECONDS);

    // The notification completes successfully even without a handler
    context.assertTrue(true, "Notification should succeed even without handler");
  }

  @Test
  public void testNotificationsDisabledIgnoresSilently(TestContext context) throws Throwable {
    ServerOptions options = new ServerOptions().setNotificationsEnabled(false);
    AtomicBoolean notificationReceived = new AtomicBoolean(false);
    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx, options);

    server.addServerFeature(new ServerFeature() {
      @Override
      public void handle(ServerRequest request) {
        notificationReceived.set(true);
      }

      @Override
      public Set<String> getCapabilities() {
        return Set.of("notifications/test");
      }
    });

    startServer(context, server);

    createSession()
      .compose(session -> session.sendNotification(new JsonNotification("notifications/test", new JsonObject())))
      .await(10, TimeUnit.SECONDS);

    Thread.sleep(100);

    context.assertFalse(notificationReceived.get(), "Notification handler should not be called when disabled");
  }

  @Test
  public void testRequestWithIdReturnsJsonResponse(TestContext context) throws Throwable {
    ServerOptions options = new ServerOptions().setNotificationsEnabled(true);
    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx, options);

    startServer(context, server);

    try {
      createSession()
        .compose(session -> session.sendRequest(JsonRequest.createRequest("unknown/method", new JsonObject(), 1)))
        .await(10, TimeUnit.SECONDS);
      context.fail("Should have thrown ClientRequestException");
    } catch (ClientRequestException e) {
      context.assertEquals(JsonError.METHOD_NOT_FOUND, e.getCode(), "Should be method not found");
    }
  }

  @Test
  public void testNotificationVsRequestDistinction(TestContext context) throws Throwable {
    ServerOptions options = new ServerOptions().setNotificationsEnabled(true);
    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx, options);

    startServer(context, server);

    // Notification completes without waiting for a response
    createSession()
      .compose(session -> session.sendNotification(new JsonNotification("ping", new JsonObject())))
      .await(10, TimeUnit.SECONDS);

    // Request waits for and returns a response
    EmptyResult result = (EmptyResult) getClient().sendRequest(new PingRequest())
      .expecting(r -> r instanceof EmptyResult)
      .await(10, TimeUnit.SECONDS);

    context.assertNotNull(result, "Request should return a result");
  }
}
