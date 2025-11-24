package io.vertx.tests.server;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.mcp.common.request.InitializeRequest;
import io.vertx.mcp.common.request.PingRequest;
import io.vertx.mcp.common.rpc.JsonNotification;
import io.vertx.mcp.server.ModelContextProtocolServer;
import io.vertx.mcp.server.ServerOptions;
import io.vertx.mcp.server.transport.http.StreamableHttpServerTransport;
import org.junit.Test;

public class StreamableHttpTransportTest extends HttpTransportTestBase {

  @Test
  public void testRequestWithSessionIdEnablesSse(TestContext context) {
    Async async = context.async();
    ServerOptions options = new ServerOptions().setSessionsEnabled(true).setStreamingEnabled(true);
    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx, options);

    startServer(context, server);

    sendRequest(HttpMethod.POST, new InitializeRequest())
      .compose(resp -> resp.body().map(body -> resp.getHeader(StreamableHttpServerTransport.MCP_SESSION_ID_HEADER)))
      .compose(sessionId -> sendStreamingRequest(HttpMethod.POST, new PingRequest().toRequest(2).toJson().toBuffer(), sessionId))
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonObject response = body.toJsonObject();
        context.assertNotNull(response, "Should receive valid JSON response");
        context.assertNotNull(response.getString("jsonrpc"), "Should have jsonrpc field");
        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testNotificationWithSessionIdDoesNotEnableSse(TestContext context) {
    Async async = context.async();
    ServerOptions options = new ServerOptions().setSessionsEnabled(true).setStreamingEnabled(true).setNotificationsEnabled(true);
    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx, options);

    startServer(context, server);

    sendRequest(HttpMethod.POST, new InitializeRequest())
      .compose(resp -> resp.body().map(body -> resp.getHeader(StreamableHttpServerTransport.MCP_SESSION_ID_HEADER)))
      .compose(sessionId -> sendRequest(HttpMethod.POST, new JsonNotification("notifications/test", new JsonObject()).toJson().toBuffer(), sessionId))
      .onComplete(context.asyncAssertSuccess(resp -> {
        context.assertEquals(200, resp.statusCode(), "Notification should return 200 even with session ID");
        String contentType = resp.getHeader("Content-Type");
        context.assertNotEquals("text/event-stream", contentType, "Should not use SSE for notifications");
        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testRequestWithoutSessionIdDoesNotEnableSse(TestContext context) {
    Async async = context.async();
    ServerOptions options = new ServerOptions().setSessionsEnabled(true).setStreamingEnabled(true);
    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx, options);

    startServer(context, server);

    sendRequest(HttpMethod.POST, new PingRequest()).onComplete(context.asyncAssertSuccess(resp -> {
      String contentType = resp.getHeader("Content-Type");
      context.assertEquals("application/json", contentType, "Should use regular JSON without session");
      context.assertNotEquals("text/event-stream", contentType);
      async.complete();
    }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testStreamingDisabledUsesRegularJson(TestContext context) {
    Async async = context.async();
    ServerOptions options = new ServerOptions().setSessionsEnabled(true).setStreamingEnabled(false);
    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx, options);

    startServer(context, server);

    sendRequest(HttpMethod.POST, new InitializeRequest())
      .compose(resp -> resp.body().map(body -> resp.getHeader(StreamableHttpServerTransport.MCP_SESSION_ID_HEADER)))
      .compose(sessionId -> sendRequest(HttpMethod.POST, new PingRequest().toRequest(2).toJson().toBuffer(), sessionId))
      .onComplete(context.asyncAssertSuccess(resp -> {
        String contentType = resp.getHeader("Content-Type");
        context.assertEquals("application/json", contentType, "Should use JSON when streaming disabled");
        async.complete();
      }));

    async.awaitSuccess(10_000);
  }
}
