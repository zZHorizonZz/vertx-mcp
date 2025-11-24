package io.vertx.tests.server;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.mcp.common.request.PingRequest;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.server.ModelContextProtocolServer;
import io.vertx.mcp.server.ServerFeature;
import io.vertx.mcp.server.ServerOptions;
import io.vertx.mcp.server.ServerRequest;
import org.junit.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class NotificationHandlingTest extends HttpTransportTestBase {

  @Test
  public void testNotificationReturns202Accepted(TestContext context) {
    Async async = context.async();

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

    JsonObject notificationJson = new JsonObject()
      .put("jsonrpc", "2.0")
      .put("method", "notifications/test")
      .put("params", new JsonObject());

    sendRequest(HttpMethod.POST, notificationJson.toBuffer()).onComplete(context.asyncAssertSuccess(resp -> {
      context.assertEquals(202, resp.statusCode(), "Notification should return 202 Accepted");

      resp.body().onComplete(context.asyncAssertSuccess(body -> {
        context.assertTrue(body.length() == 0, "Response body should be empty for notification");

        vertx.setTimer(100, tid -> {
          context.assertTrue(notificationReceived.get(), "Notification handler should have been called");
          async.complete();
        });
      }));
    }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testNotificationWithoutHandlerReturns202(TestContext context) {
    Async async = context.async();

    ServerOptions options = new ServerOptions().setNotificationsEnabled(true);
    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx, options);
    startServer(context, server);

    JsonObject notificationJson = new JsonObject()
      .put("jsonrpc", "2.0")
      .put("method", "notifications/unknown")
      .put("params", new JsonObject());

    sendRequest(HttpMethod.POST, notificationJson.toBuffer()).onComplete(context.asyncAssertSuccess(resp -> {
      context.assertEquals(200, resp.statusCode(), "Notification without handler should still return 200");

      resp.body().onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());
        context.assertNotNull(response.getError(), "Should have error for unknown method");
        context.assertEquals(-32601, response.getError().getCode(), "Should be method not found");
        async.complete();
      }));
    }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testNotificationsDisabledIgnoresSilently(TestContext context) {
    Async async = context.async();

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

    sendRequest(HttpMethod.POST, JsonRequest.createRequest("notifications/test", new JsonObject(), 1).toBuffer()).onComplete(context.asyncAssertSuccess(resp -> {
      resp.body().onComplete(context.asyncAssertSuccess(body -> vertx.setTimer(100, tid -> {
        context.assertFalse(notificationReceived.get(), "Notification handler should not be called when disabled");
        async.complete();
      })));
    }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testRequestWithIdReturnsJsonResponse(TestContext context) {
    Async async = context.async();

    ServerOptions options = new ServerOptions().setNotificationsEnabled(true);
    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx, options);
    startServer(context, server);

    sendRequest(HttpMethod.POST, JsonRequest.createRequest("unknown/method", new JsonObject(), 1)).onComplete(context.asyncAssertSuccess(resp -> {
      context.assertEquals(200, resp.statusCode(), "Request should return 200");

      resp.body().onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());
        context.assertNotNull(response.getError(), "Should have error for unknown method");
        context.assertEquals(-32601, response.getError().getCode(), "Should be method not found");
        async.complete();
      }));
    }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testNotificationVsRequestDistinction(TestContext context) {
    Async async = context.async(2);

    ServerOptions options = new ServerOptions().setNotificationsEnabled(true);
    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx, options);
    startServer(context, server);

    JsonObject notificationJson = new JsonObject()
      .put("jsonrpc", "2.0")
      .put("method", "ping")
      .put("params", new JsonObject());

    sendRequest(HttpMethod.POST, notificationJson.toBuffer()).onComplete(context.asyncAssertSuccess(resp -> {
      context.assertEquals(202, resp.statusCode());
      async.countDown();
    }));

    sendRequest(HttpMethod.POST, new PingRequest()).onComplete(context.asyncAssertSuccess(resp -> {
      context.assertEquals(200, resp.statusCode());
      async.countDown();
    }));
  }
}
