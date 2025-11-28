package io.vertx.tests.server;

import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpResponseHead;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.mcp.common.request.PingRequest;
import io.vertx.mcp.common.rpc.JsonError;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
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

    JsonObject notificationJson = new JsonObject()
      .put("jsonrpc", "2.0")
      .put("method", "notifications/test")
      .put("params", new JsonObject());

    sendRequest(HttpMethod.POST, notificationJson.toBuffer())
      .compose(resp -> {
        context.assertEquals(202, resp.statusCode(), "Notification should return 202 Accepted");
        return resp.body().map(body -> {
          context.assertTrue(body.length() == 0, "Response body should be empty for notification");
          return body;
        });
      })
      .await(10, TimeUnit.SECONDS);

    Thread.sleep(100);

    context.assertTrue(notificationReceived.get(), "Notification handler should have been called");
  }

  @Test
  public void testNotificationWithoutHandlerReturns202(TestContext context) throws Throwable {
    ServerOptions options = new ServerOptions().setNotificationsEnabled(true);
    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx, options);

    startServer(context, server);

    JsonObject notificationJson = new JsonObject()
      .put("jsonrpc", "2.0")
      .put("method", "notifications/unknown")
      .put("params", new JsonObject());

    JsonResponse response = sendRequest(HttpMethod.POST, notificationJson.toBuffer())
      .compose(resp -> {
        context.assertEquals(200, resp.statusCode(), "Notification without handler should still return 200");
        return resp.body();
      })
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .await(10, TimeUnit.SECONDS);

    context.assertNotNull(response.getError(), "Should have error for unknown method");
    context.assertEquals(JsonError.METHOD_NOT_FOUND, response.getError().getCode(), "Should be method not found");
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

    sendRequest(HttpMethod.POST, JsonRequest.createRequest("notifications/test", new JsonObject(), 1).toBuffer())
      .compose(HttpClientResponse::body)
      .await(10, TimeUnit.SECONDS);

    Thread.sleep(100);

    context.assertFalse(notificationReceived.get(), "Notification handler should not be called when disabled");
  }

  @Test
  public void testRequestWithIdReturnsJsonResponse(TestContext context) throws Throwable {
    ServerOptions options = new ServerOptions().setNotificationsEnabled(true);
    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx, options);

    startServer(context, server);

    JsonResponse response = sendRequest(HttpMethod.POST, JsonRequest.createRequest("unknown/method", new JsonObject(), 1))
      .compose(resp -> {
        context.assertEquals(200, resp.statusCode(), "Request should return 200");
        return resp.body();
      })
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .await(10, TimeUnit.SECONDS);

    context.assertNotNull(response.getError(), "Should have error for unknown method");
    context.assertEquals(JsonError.METHOD_NOT_FOUND, response.getError().getCode(), "Should be method not found");
  }

  @Test
  public void testNotificationVsRequestDistinction(TestContext context) throws Throwable {
    ServerOptions options = new ServerOptions().setNotificationsEnabled(true);
    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx, options);

    startServer(context, server);

    JsonObject notificationJson = new JsonObject()
      .put("jsonrpc", "2.0")
      .put("method", "ping")
      .put("params", new JsonObject());

    int notificationStatus = sendRequest(HttpMethod.POST, notificationJson.toBuffer())
      .map(HttpResponseHead::statusCode)
      .await(10, TimeUnit.SECONDS);

    int requestStatus = sendRequest(HttpMethod.POST, new PingRequest())
      .map(HttpResponseHead::statusCode)
      .await(10, TimeUnit.SECONDS);

    context.assertEquals(202, notificationStatus);
    context.assertEquals(200, requestStatus);
  }
}
