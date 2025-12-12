package io.vertx.mcp.it;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;
import io.vertx.mcp.client.ClientSession;
import io.vertx.mcp.common.request.CallToolRequest;
import io.vertx.mcp.common.request.PingRequest;
import io.vertx.mcp.common.result.CallToolResult;
import io.vertx.mcp.common.result.EmptyResult;
import io.vertx.mcp.server.ModelContextProtocolServer;
import io.vertx.mcp.server.ServerSession;
import io.vertx.mcp.server.feature.ProtocolServerFeature;
import io.vertx.mcp.server.feature.SessionServerFeature;
import io.vertx.mcp.server.feature.ToolServerFeature;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class SessionManagementTest extends HttpTransportTestBase {

  private static final ObjectSchemaBuilder SESSION_SCHEMA = Schemas.objectSchema().property("hasSession", Schemas.booleanSchema()).property("sessionId", Schemas.stringSchema());
  private static final ObjectSchemaBuilder HELPER_WORKS_SCHEMA = Schemas.objectSchema().property("helperWorks", Schemas.booleanSchema());

  private ToolServerFeature toolFeature;

  @Before
  public void setUpFeatures(TestContext context) {
    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx);

    server.addServerFeature(new ProtocolServerFeature());
    server.addServerFeature(new SessionServerFeature());

    toolFeature = new ToolServerFeature();
    server.addServerFeature(toolFeature);

    super.startServer(context, server);
  }

  @Test
  public void testClientSessionHasId(TestContext context) throws Throwable {
    ClientSession session = createSession().await(10, TimeUnit.SECONDS);

    context.assertNotNull(session.id(), "Client session should have an ID");
    context.assertFalse(session.id().isEmpty(), "Client session ID should not be empty");
  }

  @Test
  public void testClientSessionIsActive(TestContext context) throws Throwable {
    ClientSession session = createSession().await(10, TimeUnit.SECONDS);

    context.assertTrue(session.isActive(), "Client session should be active after creation");
  }

  @Test
  public void testPingWithSession(TestContext context) throws Throwable {
    ClientSession session = createSession().await(10, TimeUnit.SECONDS);

    EmptyResult result = (EmptyResult) session.sendRequest(new PingRequest())
      .expecting(r -> r instanceof EmptyResult)
      .await(10, TimeUnit.SECONDS);

    context.assertNotNull(result, "Ping should succeed");
  }

  @Test
  public void testServerSessionIsStoredInContext(TestContext context) throws Throwable {
    toolFeature.addStructuredTool("check-session", Schemas.objectSchema(), SESSION_SCHEMA, args -> {
      Context ctx = Vertx.currentContext();
      ServerSession session = ServerSession.fromContext(ctx);
      JsonObject result = new JsonObject()
        .put("hasSession", session != null)
        .put("sessionId", session != null ? session.id() : null);
      return Future.succeededFuture(result);
    });

    ClientSession clientSession = createSession().await(10, TimeUnit.SECONDS);

    JsonObject params = new JsonObject()
      .put("name", "check-session")
      .put("arguments", new JsonObject());

    CallToolResult result = (CallToolResult) clientSession.sendRequest(new CallToolRequest(params))
      .expecting(r -> r instanceof CallToolResult)
      .await(10, TimeUnit.SECONDS);

    context.assertFalse(result.getIsError(), "Should succeed");
    JsonObject structuredContent = result.getStructuredContent();

    context.assertTrue(structuredContent.getBoolean("hasSession"), "ServerSession should be in context");
    context.assertNotNull(structuredContent.getString("sessionId"), "ServerSession should have an ID");
  }

  @Test
  public void testServerSessionIdMatchesClientSessionId(TestContext context) throws Throwable {
    toolFeature.addStructuredTool("get-server-session-id", Schemas.objectSchema(), SESSION_SCHEMA, args -> {
      Context ctx = Vertx.currentContext();
      ServerSession session = ServerSession.fromContext(ctx);
      JsonObject result = new JsonObject()
        .put("hasSession", session != null)
        .put("sessionId", session != null ? session.id() : null);
      return Future.succeededFuture(result);
    });

    ClientSession clientSession = createSession().await(10, TimeUnit.SECONDS);
    String clientSessionId = clientSession.id();

    JsonObject params = new JsonObject()
      .put("name", "get-server-session-id")
      .put("arguments", new JsonObject());

    CallToolResult result = (CallToolResult) clientSession.sendRequest(new CallToolRequest(params))
      .expecting(r -> r instanceof CallToolResult)
      .await(10, TimeUnit.SECONDS);

    context.assertFalse(result.getIsError(), "Should succeed");
    JsonObject structuredContent = result.getStructuredContent();

    context.assertTrue(structuredContent.getBoolean("hasSession"), "ServerSession should be in context");
    context.assertEquals(clientSessionId, structuredContent.getString("sessionId"), "Server session ID should match client session ID");
  }

  @Test
  public void testServerSessionFromContextHelperMethod(TestContext context) throws Throwable {
    toolFeature.addStructuredTool("test-helper", Schemas.objectSchema(), HELPER_WORKS_SCHEMA, args -> {
      Context ctx = Vertx.currentContext();

      ServerSession sessionFromHelper = ServerSession.fromContext(ctx);
      ServerSession sessionFromDirect = ctx.get(ServerSession.MCP_SESSION_CONTEXT_KEY);

      boolean helperWorks = (sessionFromHelper != null && sessionFromHelper == sessionFromDirect);
      return Future.succeededFuture(new JsonObject().put("helperWorks", helperWorks));
    });

    ClientSession clientSession = createSession().await(10, TimeUnit.SECONDS);

    JsonObject params = new JsonObject()
      .put("name", "test-helper")
      .put("arguments", new JsonObject());

    CallToolResult result = (CallToolResult) clientSession.sendRequest(new CallToolRequest(params))
      .expecting(r -> r instanceof CallToolResult)
      .await(10, TimeUnit.SECONDS);

    context.assertFalse(result.getIsError(), "Should succeed");
    JsonObject structuredContent = result.getStructuredContent();

    context.assertTrue(structuredContent.getBoolean("helperWorks"), "ServerSession.fromContext() should work the same as direct context.get()");
  }

  @Test
  public void testMultipleSessionsHaveDifferentIds(TestContext context) throws Throwable {
    ClientSession session1 = createSession().await(10, TimeUnit.SECONDS);
    ClientSession session2 = createSession().await(10, TimeUnit.SECONDS);

    context.assertNotNull(session1.id(), "First session should have an ID");
    context.assertNotNull(session2.id(), "Second session should have an ID");
    context.assertNotEquals(session1.id(), session2.id(), "Different sessions should have different IDs");
  }

  @Test
  public void testSessionRemainsActiveAcrossMultipleRequests(TestContext context) throws Throwable {
    toolFeature.addStructuredTool("get-session-id", Schemas.objectSchema(), SESSION_SCHEMA, args -> {
      Context ctx = Vertx.currentContext();
      ServerSession session = ServerSession.fromContext(ctx);
      return Future.succeededFuture(new JsonObject()
        .put("hasSession", session != null)
        .put("sessionId", session != null ? session.id() : null));
    });

    ClientSession clientSession = createSession().await(10, TimeUnit.SECONDS);

    JsonObject params = new JsonObject()
      .put("name", "get-session-id")
      .put("arguments", new JsonObject());

    // First request
    CallToolResult result1 = (CallToolResult) clientSession.sendRequest(new CallToolRequest(params))
      .expecting(r -> r instanceof CallToolResult)
      .await(10, TimeUnit.SECONDS);

    String sessionId1 = result1.getStructuredContent().getString("sessionId");

    // Second request
    CallToolResult result2 = (CallToolResult) clientSession.sendRequest(new CallToolRequest(params))
      .expecting(r -> r instanceof CallToolResult)
      .await(10, TimeUnit.SECONDS);

    String sessionId2 = result2.getStructuredContent().getString("sessionId");

    context.assertEquals(sessionId1, sessionId2, "Session ID should remain the same across multiple requests");
    context.assertTrue(clientSession.isActive(), "Client session should still be active");
  }
}
