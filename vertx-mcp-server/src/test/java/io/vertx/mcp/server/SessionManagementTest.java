package io.vertx.mcp.server;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.json.schema.common.dsl.SchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;
import io.vertx.mcp.common.request.CallToolRequest;
import io.vertx.mcp.common.request.InitializeRequest;
import io.vertx.mcp.common.request.PingRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.server.impl.ToolServerFeature;
import io.vertx.mcp.server.transport.http.HttpServerTransport;
import org.junit.Test;

public class SessionManagementTest extends HttpTransportTestBase {

  private static final SchemaBuilder SESSION_SCHEMA = Schemas.objectSchema().property("hasSession", Schemas.booleanSchema()).property("sessionId", Schemas.stringSchema());
  private static final SchemaBuilder HELPER_WORKS_SCHEMA = Schemas.objectSchema().property("helperWorks", Schemas.booleanSchema());

  @Test
  public void testInitializeGeneratesSessionId(TestContext context) {
    Async async = context.async();
    ModelContextProtocolServer server = ModelContextProtocolServer.create(new ServerOptions().setSessionsEnabled(true));

    startServer(context, server);

    sendRequest(HttpMethod.POST, new InitializeRequest()).compose(resp -> {
      String sessionId = resp.getHeader(HttpServerTransport.MCP_SESSION_ID_HEADER);
      context.assertNotNull(sessionId, "Session ID should be generated");
      context.assertFalse(sessionId.isEmpty(), "Session ID should not be empty");

      return resp.body().map(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());
        context.assertNull(response.getError(), "Initialize should succeed");
        context.assertNotNull(response.getResult(), "Should have result");
        return sessionId;
      });
    }).onComplete(context.asyncAssertSuccess(sessionId -> async.complete()));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testInitializeWithoutSessionsDoesNotGenerateSessionId(TestContext context) {
    Async async = context.async();
    ModelContextProtocolServer server = ModelContextProtocolServer.create(new ServerOptions().setStreamingEnabled(false).setSessionsEnabled(false));

    startServer(context, server);

    sendRequest(HttpMethod.POST, new InitializeRequest()).compose(resp -> {
      String sessionId = resp.getHeader(HttpServerTransport.MCP_SESSION_ID_HEADER);
      context.assertNull(sessionId, "Session ID should not be generated when sessions disabled");

      return resp.body().map(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());
        context.assertNull(response.getError(), "Initialize should succeed");
        return null;
      });
    }).onComplete(context.asyncAssertSuccess(v -> async.complete()));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testSubsequentRequestWithSessionId(TestContext context) {
    Async async = context.async();
    ModelContextProtocolServer server = ModelContextProtocolServer.create(new ServerOptions().setSessionsEnabled(true));

    startServer(context, server);

    // First, initialize to get a session ID
    sendRequest(HttpMethod.POST, new InitializeRequest())
      .compose(resp -> resp.body().map(body -> resp.getHeader(HttpServerTransport.MCP_SESSION_ID_HEADER)))
      .compose(sessionId -> sendStreamingRequest(HttpMethod.POST, new PingRequest().toRequest(2).toJson().toBuffer(), sessionId))
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
    ModelContextProtocolServer server = ModelContextProtocolServer.create(new ServerOptions().setSessionsEnabled(true));

    startServer(context, server);

    sendRequest(HttpMethod.POST, new PingRequest(), "invalid-session-id").onComplete(context.asyncAssertSuccess(resp -> {
      context.assertEquals(400, resp.statusCode(), "Invalid session should return 400");
      async.complete();
    }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testRequestWithoutSessionIdWorks(TestContext context) {
    Async async = context.async();
    ModelContextProtocolServer server = ModelContextProtocolServer.create(new ServerOptions().setSessionsEnabled(true));

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

  @Test
  public void testSessionIsStoredInContextWithExistingSessionId(TestContext context) {
    Async async = context.async();
    ModelContextProtocolServer server = ModelContextProtocolServer.create(new ServerOptions().setSessionsEnabled(true));

    // Add tool feature to test session in context
    ToolServerFeature toolFeature = new ToolServerFeature();

    toolFeature.addStructuredTool("check-session", StructuredToolHandler.create(Schemas.objectSchema(), SESSION_SCHEMA, args -> {
        Context ctx = Vertx.currentContext();
        Session session = Session.fromContext(ctx);
        JsonObject result = new JsonObject().put("hasSession", session != null).put("sessionId", session != null ? session.id() : null);
        return Future.succeededFuture(result);
      })
    );

    server.serverFeatures(toolFeature);

    startServer(context, server);

    // First, initialize to get a session ID
    sendRequest(HttpMethod.POST, new InitializeRequest())
      .compose(resp -> resp.body().map(body -> resp.getHeader(HttpServerTransport.MCP_SESSION_ID_HEADER)))
      .compose(sessionId -> sendRequest(
        HttpMethod.POST, new CallToolRequest(new JsonObject().put("name", "check-session").put("arguments", new JsonObject())).toRequest(2), sessionId
      )
        .compose(HttpClientResponse::body)
        .map(body -> {
          JsonResponse response = JsonResponse.fromJson(body.toJsonObject());
          context.assertNull(response.getError(), "Should succeed");
          JsonObject result = (JsonObject) response.getResult();
          JsonObject structuredContent = result.getJsonObject("structuredContent");

          // Verify session was in context
          context.assertTrue(structuredContent.getBoolean("hasSession"), "Session should be in context");
          context.assertEquals(sessionId, structuredContent.getString("sessionId"), "Session ID should match");
          return null;
        }))
      .onComplete(context.asyncAssertSuccess(v -> async.complete()));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testSessionFromContextHelperMethod(TestContext context) {
    Async async = context.async();
    ModelContextProtocolServer server = ModelContextProtocolServer.create(new ServerOptions().setSessionsEnabled(true));

    ToolServerFeature toolFeature = new ToolServerFeature();

    toolFeature.addStructuredTool("test-helper", StructuredToolHandler.create(Schemas.objectSchema(), HELPER_WORKS_SCHEMA, args -> {
        // Get the current Vert.x context
        Context ctx = Vertx.currentContext();

        // Test both retrieval methods
        Session sessionFromHelper = Session.fromContext(ctx);
        Session sessionFromDirect = ctx.get(HttpServerTransport.MCP_SESSION_CONTEXT_KEY);

        boolean helperWorks = (sessionFromHelper != null && sessionFromHelper == sessionFromDirect);
        return Future.succeededFuture(new JsonObject().put("helperWorks", helperWorks));
      })
    );

    server.serverFeatures(toolFeature);

    startServer(context, server);

    // First, initialize to get a session ID
    sendRequest(HttpMethod.POST, new InitializeRequest())
      .compose(resp -> resp.body().map(body -> resp.getHeader(HttpServerTransport.MCP_SESSION_ID_HEADER)))
      .compose(sessionId -> sendRequest(
        HttpMethod.POST, new CallToolRequest(new JsonObject().put("name", "test-helper").put("arguments", new JsonObject())).toRequest(2), sessionId
      )
        .compose(HttpClientResponse::body)
        .map(body -> {
          JsonResponse response = JsonResponse.fromJson(body.toJsonObject());
          context.assertNull(response.getError(), "Should succeed");
          JsonObject result = (JsonObject) response.getResult();
          JsonObject structuredContent = result.getJsonObject("structuredContent");

          context.assertTrue(structuredContent.getBoolean("helperWorks"), "Session.fromContext() should work the same as direct context.get()");
          return null;
        }))
      .onComplete(context.asyncAssertSuccess(v -> async.complete()));

    async.awaitSuccess(10_000);
  }
}
