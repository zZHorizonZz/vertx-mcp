package io.vertx.tests.mcp.server;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.mcp.common.request.InitializeRequest;
import io.vertx.mcp.common.request.PingRequest;
import io.vertx.mcp.common.rpc.JsonNotification;
import io.vertx.mcp.server.ModelContextProtocolServer;
import io.vertx.mcp.server.ServerOptions;
import io.vertx.mcp.server.transport.http.StreamableHttpServerTransport;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class StreamableHttpTransportTest extends HttpTransportTestBase {

  @Test
  public void testRequestWithSessionIdEnablesStreaming(TestContext context) throws Throwable {
    ServerOptions options = new ServerOptions().setSessionsEnabled(true).setStreamingEnabled(true);
    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx, options);

    startServer(context, server);

    String sessionId = sendRequest(HttpMethod.POST, new InitializeRequest())
      .compose(resp -> resp.body().map(body -> resp.getHeader(StreamableHttpServerTransport.MCP_SESSION_ID_HEADER)))
      .await(10, TimeUnit.SECONDS);

    JsonObject response = sendStreamingRequest(HttpMethod.POST, new PingRequest().toRequest(2).toJson().toBuffer(), sessionId)
      .map(Buffer::toJsonObject)
      .await(10, TimeUnit.SECONDS);

    context.assertNotNull(response, "Should receive valid JSON response");
    context.assertNotNull(response.getString("jsonrpc"), "Should have jsonrpc field");
  }

  @Test
  public void testNotificationWithSessionIdDoesNotEnableStreaming(TestContext context) throws Throwable {
    ServerOptions options = new ServerOptions().setSessionsEnabled(true).setStreamingEnabled(true).setNotificationsEnabled(true);
    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx, options);

    startServer(context, server);

    String sessionId = sendRequest(HttpMethod.POST, new InitializeRequest())
      .compose(resp -> resp.body().map(body -> resp.getHeader(StreamableHttpServerTransport.MCP_SESSION_ID_HEADER)))
      .await(10, TimeUnit.SECONDS);

    HttpClientResponse resp = sendRequest(HttpMethod.POST, new JsonNotification("notifications/test", new JsonObject()).toJson().toBuffer(), sessionId).await(10, TimeUnit.SECONDS);

    context.assertEquals(200, resp.statusCode(), "Notification should return 200 even with session ID");
    String contentType = resp.getHeader(HttpHeaders.CONTENT_TYPE);
    context.assertNotEquals("text/event-stream", contentType, "Should not use streaming transport for notifications");
  }

  @Test
  public void testRequestWithoutSessionIdDoesNotEnableStreaming(TestContext context) throws Throwable {
    ServerOptions options = new ServerOptions().setSessionsEnabled(true).setStreamingEnabled(true);
    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx, options);

    startServer(context, server);

    HttpClientResponse resp = sendRequest(HttpMethod.POST, new PingRequest()).await(10, TimeUnit.SECONDS);

    String contentType = resp.getHeader(HttpHeaders.CONTENT_TYPE);
    context.assertEquals("application/json", contentType, "Should use regular JSON without session");
    context.assertNotEquals("text/event-stream", contentType);
  }

  @Test
  public void testStreamingDisabledUsesRegularJson(TestContext context) throws Throwable {
    ServerOptions options = new ServerOptions().setSessionsEnabled(true).setStreamingEnabled(false);
    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx, options);

    startServer(context, server);

    String sessionId = sendRequest(HttpMethod.POST, new InitializeRequest())
      .compose(resp -> resp.body().map(body -> resp.getHeader(StreamableHttpServerTransport.MCP_SESSION_ID_HEADER)))
      .await(10, TimeUnit.SECONDS);

    HttpClientResponse resp = sendRequest(HttpMethod.POST, new PingRequest().toRequest(2).toJson().toBuffer(), sessionId)
      .await(10, TimeUnit.SECONDS);

    String contentType = resp.getHeader(HttpHeaders.CONTENT_TYPE);
    context.assertEquals("application/json", contentType, "Should use JSON when streaming disabled");
  }
}
