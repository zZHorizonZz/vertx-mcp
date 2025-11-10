package io.vertx.mcp.server;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.mcp.common.request.InitializeRequest;
import io.vertx.mcp.common.request.PingRequest;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.server.transport.http.HttpServerTransport;
import org.junit.Test;

public class SseTransportTest extends HttpTransportTestBase {

  @Test
  public void testRequestWithSessionIdEnablesSse(TestContext context) {
    ServerOptions options = new ServerOptions()
      .setSessionsEnabled(true)
      .setStreamingEnabled(true);

    ModelContextProtocolServer server = ModelContextProtocolServer.create(options);
    startServer(context, server);

    Async async = context.async();

    // First, initialize to get a session ID
    JsonRequest initRequest = new InitializeRequest().toRequest(1);

    sendRequest(HttpMethod.POST, initRequest.toJson().toBuffer())
      .compose(resp -> resp.body().map(body -> resp.getHeader(HttpServerTransport.MCP_SESSION_ID_HEADER)))
      .compose(sessionId -> {
        // Now make a request with the session ID (not a notification)
        JsonRequest pingRequest = new PingRequest().toRequest(2);
        return sendStreamingRequest(HttpMethod.POST, pingRequest.toJson().toBuffer(), sessionId);
      })
      .onComplete(context.asyncAssertSuccess(body -> {
        // Verify we got valid JSON response
        JsonObject response = body.toJsonObject();
        context.assertNotNull(response, "Should receive valid JSON response");
        context.assertNotNull(response.getString("jsonrpc"), "Should have jsonrpc field");
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

    Async async = context.async();

    // First, initialize to get a session ID
    JsonRequest initRequest = new InitializeRequest().toRequest(1);

    sendRequest(HttpMethod.POST, initRequest.toJson().toBuffer())
      .compose(resp -> resp.body().map(body -> resp.getHeader(HttpServerTransport.MCP_SESSION_ID_HEADER)))
      .compose(sessionId -> {
        // Now send a notification with the session ID
        JsonObject notificationJson = new JsonObject()
          .put("jsonrpc", "2.0")
          .put("method", "notifications/test")
          .put("params", new JsonObject());

        return sendRequest(HttpMethod.POST, notificationJson.toBuffer(), sessionId);
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

    Async async = context.async();

    // Ping without session ID
    JsonRequest pingRequest = new PingRequest().toRequest(1);

    sendRequest(HttpMethod.POST, pingRequest.toJson().toBuffer())
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

    Async async = context.async();

    // Initialize to get session
    JsonRequest initRequest = new InitializeRequest().toRequest(1);

    sendRequest(HttpMethod.POST, initRequest.toJson().toBuffer())
      .compose(resp -> resp.body().map(body -> resp.getHeader(HttpServerTransport.MCP_SESSION_ID_HEADER)))
      .compose(sessionId -> {
        // Request with session ID but streaming disabled
        JsonRequest pingRequest = new PingRequest().toRequest(2);
        return sendRequest(HttpMethod.POST, pingRequest.toJson().toBuffer(), sessionId);
      })
      .onComplete(context.asyncAssertSuccess(resp -> {
        String contentType = resp.getHeader("Content-Type");
        context.assertEquals("application/json", contentType, "Should use JSON when streaming disabled");
        async.complete();
      }));

    async.awaitSuccess(10_000);
  }
}
