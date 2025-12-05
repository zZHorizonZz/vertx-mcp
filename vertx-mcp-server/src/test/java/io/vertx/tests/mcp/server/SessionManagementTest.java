package io.vertx.tests.mcp.server;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;
import io.vertx.mcp.common.request.CallToolRequest;
import io.vertx.mcp.common.request.InitializeRequest;
import io.vertx.mcp.common.request.PingRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.server.ModelContextProtocolServer;
import io.vertx.mcp.server.ServerOptions;
import io.vertx.mcp.server.ServerSession;
import io.vertx.mcp.server.feature.ToolServerFeature;
import io.vertx.mcp.server.transport.http.StreamableHttpServerTransport;
import org.junit.Test;

public class SessionManagementTest extends HttpTransportTestBase {

  private static final ObjectSchemaBuilder SESSION_SCHEMA = Schemas.objectSchema().property("hasSession", Schemas.booleanSchema()).property("sessionId", Schemas.stringSchema());
  private static final ObjectSchemaBuilder HELPER_WORKS_SCHEMA = Schemas.objectSchema().property("helperWorks", Schemas.booleanSchema());

  @Test
  public void testInitializeGeneratesSessionId(TestContext context) {
    Async async = context.async();
    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx, new ServerOptions().setSessionsEnabled(true));

    startServer(context, server);

    sendRequest(HttpMethod.POST, new InitializeRequest()).compose(resp -> {
      String sessionId = resp.getHeader(StreamableHttpServerTransport.MCP_SESSION_ID_HEADER);
      context.assertNotNull(sessionId, "ServerSession ID should be generated");
      context.assertFalse(sessionId.isEmpty(), "ServerSession ID should not be empty");

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
    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx, new ServerOptions().setStreamingEnabled(false).setSessionsEnabled(false));

    startServer(context, server);

    sendRequest(HttpMethod.POST, new InitializeRequest()).compose(resp -> {
      String sessionId = resp.getHeader(StreamableHttpServerTransport.MCP_SESSION_ID_HEADER);
      context.assertNull(sessionId, "ServerSession ID should not be generated when sessions disabled");

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
    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx, new ServerOptions().setSessionsEnabled(true));

    startServer(context, server);

    sendRequest(HttpMethod.POST, new InitializeRequest())
      .compose(resp -> resp.body().map(body -> resp.getHeader(StreamableHttpServerTransport.MCP_SESSION_ID_HEADER)))
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
    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx, new ServerOptions().setSessionsEnabled(true));

    startServer(context, server);

    sendRequest(HttpMethod.POST, new PingRequest(), "invalid-session-id").onComplete(context.asyncAssertSuccess(resp -> {
      context.assertEquals(404, resp.statusCode(), "Session not found should return 404");
      async.complete();
    }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testRequestWithoutSessionIdWorks(TestContext context) {
    Async async = context.async();
    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx, new ServerOptions().setSessionsEnabled(true));

    startServer(context, server);

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
    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx, new ServerOptions().setSessionsEnabled(true));

    ToolServerFeature toolFeature = new ToolServerFeature();

    toolFeature.addStructuredTool("check-session", Schemas.objectSchema(), SESSION_SCHEMA, args -> {
        Context ctx = Vertx.currentContext();
        ServerSession session = ServerSession.fromContext(ctx);
        JsonObject result = new JsonObject().put("hasSession", session != null).put("sessionId", session != null ? session.id() : null);
        return Future.succeededFuture(result);
      }
    );

    server.addServerFeature(toolFeature);

    startServer(context, server);

    sendRequest(HttpMethod.POST, new InitializeRequest())
      .compose(resp -> resp.body().map(body -> resp.getHeader(StreamableHttpServerTransport.MCP_SESSION_ID_HEADER)))
      .compose(sessionId -> sendRequest(
        HttpMethod.POST, new CallToolRequest(new JsonObject().put("name", "check-session").put("arguments", new JsonObject())).toRequest(2), sessionId
      )
        .compose(HttpClientResponse::body)
        .map(body -> {
          JsonResponse response = JsonResponse.fromJson(body.toJsonObject());
          context.assertNull(response.getError(), "Should succeed");
          JsonObject result = (JsonObject) response.getResult();
          JsonObject structuredContent = result.getJsonObject("structuredContent");

          context.assertTrue(structuredContent.getBoolean("hasSession"), "ServerSession should be in context");
          context.assertEquals(sessionId, structuredContent.getString("sessionId"), "ServerSession ID should match");
          return null;
        }))
      .onComplete(context.asyncAssertSuccess(v -> async.complete()));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testSessionFromContextHelperMethod(TestContext context) {
    Async async = context.async();
    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx, new ServerOptions().setSessionsEnabled(true));

    ToolServerFeature toolFeature = new ToolServerFeature();

    toolFeature.addStructuredTool("test-helper", Schemas.objectSchema(), HELPER_WORKS_SCHEMA, args -> {
        Context ctx = Vertx.currentContext();

        ServerSession sessionFromHelper = ServerSession.fromContext(ctx);
        ServerSession sessionFromDirect = ctx.get(ServerSession.MCP_SESSION_CONTEXT_KEY);

        boolean helperWorks = (sessionFromHelper != null && sessionFromHelper == sessionFromDirect);
        return Future.succeededFuture(new JsonObject().put("helperWorks", helperWorks));
      }
    );

    server.addServerFeature(toolFeature);

    startServer(context, server);

    sendRequest(HttpMethod.POST, new InitializeRequest())
      .compose(resp -> resp.body().map(body -> resp.getHeader(StreamableHttpServerTransport.MCP_SESSION_ID_HEADER)))
      .compose(sessionId -> sendRequest(
        HttpMethod.POST, new CallToolRequest(new JsonObject().put("name", "test-helper").put("arguments", new JsonObject())).toRequest(2), sessionId
      )
        .compose(HttpClientResponse::body)
        .map(body -> {
          JsonResponse response = JsonResponse.fromJson(body.toJsonObject());
          context.assertNull(response.getError(), "Should succeed");
          JsonObject result = (JsonObject) response.getResult();
          JsonObject structuredContent = result.getJsonObject("structuredContent");

          context.assertTrue(structuredContent.getBoolean("helperWorks"), "ServerSession.fromContext() should work the same as direct context.get()");
          return null;
        }))
      .onComplete(context.asyncAssertSuccess(v -> async.complete()));

    async.awaitSuccess(10_000);
  }
}
