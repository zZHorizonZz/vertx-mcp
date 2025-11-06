package io.vertx.mcp.server;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.mcp.common.request.PingRequest;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
import org.junit.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class NotificationHandlingTest extends HttpTransportTestBase {

  @Test
  public void testNotificationReturns202Accepted(TestContext context) {
    ServerOptions options = new ServerOptions()
      .setNotificationsEnabled(true);

    ModelContextProtocolServer server = ModelContextProtocolServer.create(options);

    // Add a feature that handles notifications
    AtomicBoolean notificationReceived = new AtomicBoolean(false);
    server.serverFeatures(new ServerFeature() {
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

    HttpClient client = vertx.createHttpClient(new HttpClientOptions());
    Async async = context.async();

    // Send a notification (no id field)
    JsonObject notificationJson = new JsonObject()
      .put("jsonrpc", "2.0")
      .put("method", "notifications/test")
      .put("params", new JsonObject());

    client.request(HttpMethod.POST, port, "localhost", "/")
      .compose(req -> {
        req.putHeader("Content-Type", "application/json");
        return req.send(notificationJson.toBuffer());
      })
      .onComplete(context.asyncAssertSuccess(resp -> {
        context.assertEquals(202, resp.statusCode(), "Notification should return 202 Accepted");

        // Verify body is empty
        resp.body().onComplete(context.asyncAssertSuccess(body -> {
          context.assertTrue(body.length() == 0, "Response body should be empty for notification");

          // Give the notification handler time to execute
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
    ServerOptions options = new ServerOptions()
      .setNotificationsEnabled(true);

    ModelContextProtocolServer server = ModelContextProtocolServer.create(options);
    startServer(context, server);

    HttpClient client = vertx.createHttpClient(new HttpClientOptions());
    Async async = context.async();

    // Send a notification without a handler
    JsonObject notificationJson = new JsonObject()
      .put("jsonrpc", "2.0")
      .put("method", "notifications/unknown")
      .put("params", new JsonObject());

    client.request(HttpMethod.POST, port, "localhost", "/")
      .compose(req -> {
        req.putHeader("Content-Type", "application/json");
        return req.send(notificationJson.toBuffer());
      })
      .onComplete(context.asyncAssertSuccess(resp -> {
        context.assertEquals(202, resp.statusCode(), "Notification without handler should still return 202");
        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testNotificationsDisabledIgnoresSilently(TestContext context) {
    ServerOptions options = new ServerOptions()
      .setNotificationsEnabled(false);

    AtomicBoolean notificationReceived = new AtomicBoolean(false);
    ModelContextProtocolServer server = ModelContextProtocolServer.create(options);
    server.serverFeatures(new ServerFeature() {
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

    HttpClient client = vertx.createHttpClient(new HttpClientOptions());
    Async async = context.async();

    // Send a notification
    JsonObject notificationJson = new JsonObject()
      .put("jsonrpc", "2.0")
      .put("method", "notifications/test")
      .put("params", new JsonObject());

    client.request(HttpMethod.POST, port, "localhost", "/")
      .compose(req -> {
        req.putHeader("Content-Type", "application/json");
        return req.send(notificationJson.toBuffer());
      })
      .onComplete(context.asyncAssertSuccess(resp -> {
        // Notification should be ignored but connection should not fail
        resp.body().onComplete(context.asyncAssertSuccess(body -> {
          vertx.setTimer(100, tid -> {
            context.assertFalse(notificationReceived.get(), "Notification handler should not be called when disabled");
            async.complete();
          });
        }));
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testRequestWithIdReturnsJsonResponse(TestContext context) {
    ServerOptions options = new ServerOptions()
      .setNotificationsEnabled(true);

    ModelContextProtocolServer server = ModelContextProtocolServer.create(options);
    startServer(context, server);

    HttpClient client = vertx.createHttpClient(new HttpClientOptions());
    Async async = context.async();

    // Send a request (has id field) to unknown method
    JsonRequest request = JsonRequest.createRequest("unknown/method", new JsonObject(), 1);

    client.request(HttpMethod.POST, port, "localhost", "/")
      .compose(req -> {
        req.putHeader("Content-Type", "application/json");
        return req.send(request.toJson().toBuffer())
          .compose(resp -> {
            context.assertEquals(200, resp.statusCode(), "Request should return 200");
            return resp.body();
          });
      })
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());
        context.assertNotNull(response.getError(), "Should have error for unknown method");
        context.assertEquals(-32601, response.getError().getCode(), "Should be method not found");
        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testNotificationVsRequestDistinction(TestContext context) {
    ServerOptions options = new ServerOptions()
      .setNotificationsEnabled(true);

    ModelContextProtocolServer server = ModelContextProtocolServer.create(options);
    startServer(context, server);

    HttpClient client = vertx.createHttpClient(new HttpClientOptions());
    Async async = context.async(2);

    // Test 1: Notification (no id)
    JsonObject notificationJson = new JsonObject()
      .put("jsonrpc", "2.0")
      .put("method", "ping")
      .put("params", new JsonObject());

    client.request(HttpMethod.POST, port, "localhost", "/")
      .compose(req -> {
        req.putHeader("Content-Type", "application/json");
        return req.send(notificationJson.toBuffer());
      })
      .onComplete(context.asyncAssertSuccess(resp -> {
        context.assertEquals(202, resp.statusCode());
        async.countDown();
      }));

    // Test 2: Request (has id)
    JsonRequest request = new PingRequest().toRequest(1);

    client.request(HttpMethod.POST, port, "localhost", "/")
      .compose(req -> {
        req.putHeader("Content-Type", "application/json");
        return req.send(request.toJson().toBuffer());
      })
      .onComplete(context.asyncAssertSuccess(resp -> {
        context.assertEquals(200, resp.statusCode());
        async.countDown();
      }));
  }
}
