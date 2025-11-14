package io.vertx.mcp.server;

import io.vertx.core.http.HttpMethod;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.mcp.common.request.InitializeRequest;
import io.vertx.mcp.common.request.PingRequest;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.server.transport.http.HttpServerTransport;
import org.junit.Test;

public class SessionManagementTest extends HttpTransportTestBase {

  @Test
  public void testInitializeGeneratesSessionId(TestContext context) {
    Async async = context.async();

    ServerOptions options = new ServerOptions()
      .setSessionsEnabled(true);

    ModelContextProtocolServer server = ModelContextProtocolServer.create(options);
    startServer(context, server);

    sendRequest(HttpMethod.POST, new InitializeRequest())
      .compose(resp -> {
        // Verify session ID header is present
        String sessionId = resp.getHeader(HttpServerTransport.MCP_SESSION_ID_HEADER);
        context.assertNotNull(sessionId, "Session ID should be generated");
        context.assertFalse(sessionId.isEmpty(), "Session ID should not be empty");

        return resp.body().map(body -> {
          JsonResponse response = JsonResponse.fromJson(body.toJsonObject());
          context.assertNull(response.getError(), "Initialize should succeed");
          context.assertNotNull(response.getResult(), "Should have result");
          return sessionId;
        });
      })
      .onComplete(context.asyncAssertSuccess(sessionId -> {
        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testInitializeWithoutSessionsDoesNotGenerateSessionId(TestContext context) {
    Async async = context.async();

    ServerOptions options = new ServerOptions()
      .setStreamingEnabled(false)
      .setSessionsEnabled(false);

    ModelContextProtocolServer server = ModelContextProtocolServer.create(options);
    startServer(context, server);

    sendRequest(HttpMethod.POST, new InitializeRequest())
      .compose(resp -> {
        // Verify no session ID header
        String sessionId = resp.getHeader(HttpServerTransport.MCP_SESSION_ID_HEADER);
        context.assertNull(sessionId, "Session ID should not be generated when sessions disabled");

        return resp.body().map(body -> {
          JsonResponse response = JsonResponse.fromJson(body.toJsonObject());
          context.assertNull(response.getError(), "Initialize should succeed");
          return null;
        });
      })
      .onComplete(context.asyncAssertSuccess(v -> {
        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testSubsequentRequestWithSessionId(TestContext context) {
    Async async = context.async();

    ServerOptions options = new ServerOptions()
      .setSessionsEnabled(true);

    ModelContextProtocolServer server = ModelContextProtocolServer.create(options);
    startServer(context, server);

    // First, initialize to get a session ID
    sendRequest(HttpMethod.POST, new InitializeRequest())
      .compose(resp -> resp.body().map(body -> resp.getHeader(HttpServerTransport.MCP_SESSION_ID_HEADER)))
      .compose(sessionId -> {
        // Now make another request with the session ID (using streaming since it's enabled by default)
        JsonRequest pingRequest = new PingRequest().toRequest(2);
        return sendStreamingRequest(HttpMethod.POST, pingRequest.toJson().toBuffer(), sessionId);
      })
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());
        context.assertNull(response.getError(), "Ping should succeed");
        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testInvalidSessionIdReturns400(TestContext context) {
    Async async = context.async();

    ServerOptions options = new ServerOptions()
      .setSessionsEnabled(true);

    ModelContextProtocolServer server = ModelContextProtocolServer.create(options);
    startServer(context, server);

    JsonRequest pingRequest = new PingRequest().toRequest(1);

    sendRequest(HttpMethod.POST, pingRequest.toJson().toBuffer(), "invalid-session-id")
      .onComplete(context.asyncAssertSuccess(resp -> {
        context.assertEquals(400, resp.statusCode(), "Invalid session should return 400");
        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testRequestWithoutSessionIdWorks(TestContext context) {
    Async async = context.async();

    ServerOptions options = new ServerOptions()
      .setSessionsEnabled(true);

    ModelContextProtocolServer server = ModelContextProtocolServer.create(options);
    startServer(context, server);

    // Ping without session ID should still work
    sendRequest(HttpMethod.POST, new PingRequest())
      .compose(resp -> {
        context.assertEquals(200, resp.statusCode());
        return resp.body();
      })
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());
        context.assertNull(response.getError(), "Ping should succeed without session");
        async.complete();
      }));
  }
}
