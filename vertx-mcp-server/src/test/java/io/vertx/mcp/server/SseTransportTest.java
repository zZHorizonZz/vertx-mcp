package io.vertx.mcp.server;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.mcp.common.rpc.JsonRequest;
import org.junit.Test;

public class SseTransportTest extends HttpTransportTestBase {

  private static final String MCP_SESSION_ID_HEADER = "Mcp-Session-Id";

  @Test
  public void testRequestWithSessionIdEnablesSse(TestContext context) {
    ServerOptions options = new ServerOptions()
      .setSessionsEnabled(true)
      .setStreamingEnabled(true);

    ModelContextProtocolServer server = ModelContextProtocolServer.create(options);
    startServer(context, server);

    HttpClient client = vertx.createHttpClient(new HttpClientOptions());
    Async async = context.async();

    // First, initialize to get a session ID
    JsonRequest initRequest = JsonRequest.createRequest("initialize", new JsonObject(), 1);

    client.request(HttpMethod.POST, port, "localhost", "/")
      .compose(req -> {
        req.putHeader("Content-Type", "application/json");
        return req.send(initRequest.toJson().toBuffer())
          .compose(resp -> resp.body().map(body -> resp.getHeader(MCP_SESSION_ID_HEADER)));
      })
      .compose(sessionId -> {
        // Now make a request with the session ID (not a notification)
        JsonRequest pingRequest = JsonRequest.createRequest("ping", new JsonObject(), 2);

        return client.request(HttpMethod.POST, port, "localhost", "/")
          .compose(req -> {
            req.putHeader("Content-Type", "application/json");
            req.putHeader(MCP_SESSION_ID_HEADER, sessionId);
            return req.send(pingRequest.toJson().toBuffer())
              .compose(resp -> {
                // Should enable SSE
                String contentType = resp.getHeader("Content-Type");
                context.assertEquals("text/event-stream", contentType, "Should use SSE");
                context.assertEquals("no-cache", resp.getHeader("Cache-Control"));
                context.assertEquals("keep-alive", resp.getHeader("Connection"));
                return resp.body();
              });
          });
      })
      .onComplete(context.asyncAssertSuccess(body -> {
        // SSE data should be prefixed with "data: "
        String data = body.toString();
        context.assertTrue(data.startsWith("data: "), "SSE response should start with 'data: '");
        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testNotificationWithSessionIdDoesNotEnableSse(TestContext context) {
    ServerOptions options = new ServerOptions()
      .setSessionsEnabled(true)
      .setStreamingEnabled(true)
      .setNotificationsEnabled(true);

    ModelContextProtocolServer server = ModelContextProtocolServer.create(options);
    startServer(context, server);

    HttpClient client = vertx.createHttpClient(new HttpClientOptions());
    Async async = context.async();

    // First, initialize to get a session ID
    JsonRequest initRequest = JsonRequest.createRequest("initialize", new JsonObject(), 1);

    client.request(HttpMethod.POST, port, "localhost", "/")
      .compose(req -> {
        req.putHeader("Content-Type", "application/json");
        return req.send(initRequest.toJson().toBuffer())
          .compose(resp -> resp.body().map(body -> resp.getHeader(MCP_SESSION_ID_HEADER)));
      })
      .compose(sessionId -> {
        // Now send a notification with the session ID
        JsonObject notificationJson = new JsonObject()
          .put("jsonrpc", "2.0")
          .put("method", "notifications/test")
          .put("params", new JsonObject());

        return client.request(HttpMethod.POST, port, "localhost", "/")
          .compose(req -> {
            req.putHeader("Content-Type", "application/json");
            req.putHeader(MCP_SESSION_ID_HEADER, sessionId);
            return req.send(notificationJson.toBuffer());
          });
      })
      .onComplete(context.asyncAssertSuccess(resp -> {
        // Notification should return 202, not SSE
        context.assertEquals(202, resp.statusCode(), "Notification should return 202 even with session ID");
        String contentType = resp.getHeader("Content-Type");
        context.assertNotEquals("text/event-stream", contentType, "Should not use SSE for notifications");
        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testRequestWithoutSessionIdDoesNotEnableSse(TestContext context) {
    ServerOptions options = new ServerOptions()
      .setSessionsEnabled(true)
      .setStreamingEnabled(true);

    ModelContextProtocolServer server = ModelContextProtocolServer.create(options);
    startServer(context, server);

    HttpClient client = vertx.createHttpClient(new HttpClientOptions());
    Async async = context.async();

    // Ping without session ID
    JsonRequest pingRequest = JsonRequest.createRequest("ping", new JsonObject(), 1);

    client.request(HttpMethod.POST, port, "localhost", "/")
      .compose(req -> {
        req.putHeader("Content-Type", "application/json");
        return req.send(pingRequest.toJson().toBuffer());
      })
      .onComplete(context.asyncAssertSuccess(resp -> {
        String contentType = resp.getHeader("Content-Type");
        context.assertEquals("application/json", contentType, "Should use regular JSON without session");
        context.assertNotEquals("text/event-stream", contentType);
        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testStreamingDisabledUsesRegularJson(TestContext context) {
    ServerOptions options = new ServerOptions()
      .setSessionsEnabled(true)
      .setStreamingEnabled(false);

    ModelContextProtocolServer server = ModelContextProtocolServer.create(options);
    startServer(context, server);

    HttpClient client = vertx.createHttpClient(new HttpClientOptions());
    Async async = context.async();

    // Initialize to get session
    JsonRequest initRequest = JsonRequest.createRequest("initialize", new JsonObject(), 1);

    client.request(HttpMethod.POST, port, "localhost", "/")
      .compose(req -> {
        req.putHeader("Content-Type", "application/json");
        return req.send(initRequest.toJson().toBuffer())
          .compose(resp -> resp.body().map(body -> resp.getHeader(MCP_SESSION_ID_HEADER)));
      })
      .compose(sessionId -> {
        // Request with session ID but streaming disabled
        JsonRequest pingRequest = JsonRequest.createRequest("ping", new JsonObject(), 2);

        return client.request(HttpMethod.POST, port, "localhost", "/")
          .compose(req -> {
            req.putHeader("Content-Type", "application/json");
            req.putHeader(MCP_SESSION_ID_HEADER, sessionId);
            return req.send(pingRequest.toJson().toBuffer());
          });
      })
      .onComplete(context.asyncAssertSuccess(resp -> {
        String contentType = resp.getHeader("Content-Type");
        context.assertEquals("application/json", contentType, "Should use JSON when streaming disabled");
        async.complete();
      }));

    async.awaitSuccess(10_000);
  }
}
