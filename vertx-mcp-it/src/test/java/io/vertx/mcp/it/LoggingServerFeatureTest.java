package io.vertx.mcp.it;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.json.schema.common.dsl.Schemas;
import io.vertx.mcp.client.ClientRequestException;
import io.vertx.mcp.client.ClientSession;
import io.vertx.mcp.common.LoggingLevel;
import io.vertx.mcp.common.notification.LoggingMessageNotification;
import io.vertx.mcp.common.request.CallToolRequest;
import io.vertx.mcp.common.request.SetLevelRequest;
import io.vertx.mcp.common.result.CallToolResult;
import io.vertx.mcp.common.result.EmptyResult;
import io.vertx.mcp.common.rpc.JsonError;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.server.ModelContextProtocolServer;
import io.vertx.mcp.server.ServerSession;
import io.vertx.mcp.server.feature.ToolServerFeature;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class LoggingServerFeatureTest extends HttpTransportTestBase {

  @Before
  public void setUpFeatures(TestContext context) {
    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx);
    super.startServer(context, server);
  }

  @Test
  public void testSetLevelValidLevels(TestContext context) throws Throwable {
    LoggingLevel[] levels = {
      LoggingLevel.DEBUG,
      LoggingLevel.INFO,
      LoggingLevel.NOTICE,
      LoggingLevel.WARNING,
      LoggingLevel.ERROR,
      LoggingLevel.CRITICAL,
      LoggingLevel.ALERT,
      LoggingLevel.EMERGENCY
    };

    ClientSession session = createSession().await(10, TimeUnit.SECONDS);

    for (LoggingLevel level : levels) {
      EmptyResult result = (EmptyResult) getClient().sendRequest(new SetLevelRequest().setLevel(level), session)
        .expecting(r -> r instanceof EmptyResult)
        .await(10, TimeUnit.SECONDS);

      context.assertNotNull(result, "Should have result for level: " + level.getValue());
    }
  }

  @Test
  public void testSetLevelMissingLevel(TestContext context) throws Throwable {
    ClientSession session = createSession().await(10, TimeUnit.SECONDS);

    try {
      getClient().sendRequest(new SetLevelRequest(), session).await(10, TimeUnit.SECONDS);
      context.fail("Should have thrown ClientRequestException");
    } catch (ClientRequestException e) {
      context.assertEquals(JsonError.INVALID_PARAMS, e.getCode(), "Should be invalid params");
      context.assertTrue(e.getMessage().contains("level"));
    }
  }

  @Test
  public void testSetLevelMissingParams(TestContext context) throws Throwable {
    ClientSession session = createSession().await(10, TimeUnit.SECONDS);

    try {
      session.sendRequest(JsonRequest.createRequest("logging/setLevel", null, 1)).await(10, TimeUnit.SECONDS);
      context.fail("Should have thrown ClientRequestException");
    } catch (ClientRequestException e) {
      context.assertEquals(JsonError.INVALID_PARAMS, e.getCode(), "Should be invalid params");
    }
  }

  @Test
  public void testSetLevelRequiresSession(TestContext context) throws Throwable {
    try {
      getClient().sendRequest(new SetLevelRequest().setLevel(LoggingLevel.INFO)).await(10, TimeUnit.SECONDS);
      context.fail("Should have thrown ClientRequestException");
    } catch (ClientRequestException e) {
      context.assertEquals(JsonError.INVALID_REQUEST, e.getCode(), "Should be invalid request");
      context.assertTrue(e.getMessage().contains("session"));
    }
  }

  @Test
  public void testMultipleLevelChanges(TestContext context) throws Throwable {
    ClientSession session = createSession().await(10, TimeUnit.SECONDS);

    getClient().sendRequest(new SetLevelRequest().setLevel(LoggingLevel.DEBUG), session)
      .expecting(r -> r instanceof EmptyResult)
      .await(10, TimeUnit.SECONDS);

    getClient().sendRequest(new SetLevelRequest().setLevel(LoggingLevel.ERROR), session)
      .expecting(r -> r instanceof EmptyResult)
      .await(10, TimeUnit.SECONDS);
  }

  @Test
  public void testLogConvenienceMethods(TestContext context) throws Throwable {
    ToolServerFeature toolFeature = new ToolServerFeature();

    toolFeature.addStructuredTool(
      "test-logging",
      Schemas.objectSchema(),
      Schemas.objectSchema().requiredProperty("logCount", Schemas.intSchema()),
      args -> {
        Context ctx = Vertx.currentContext();
        ServerSession session = ServerSession.fromContext(ctx);

        if (session == null) {
          return Future.succeededFuture(new JsonObject().put("logCount", 0));
        }

        // Set log level to DEBUG so all messages get through
        session.setLoggingLevel(LoggingLevel.DEBUG);

        // Test all logging convenience methods
        session.debug("test-logger", "debug message");
        session.info("test-logger", "info message");
        session.warning("test-logger", "warning message");
        session.error("test-logger", "error message");

        // Log with structured data
        session.log(LoggingLevel.INFO, "structured", new JsonObject()
          .put("key", "value")
          .put("count", 42));

        Promise<JsonObject> promise = Promise.promise();
        ctx.owner().setTimer(100, timerId -> promise.complete(new JsonObject().put("logCount", 5)));
        return promise.future();
      }
    );

    server.addServerFeature(toolFeature);

    List<LoggingMessageNotification> logNotifications = new ArrayList<>();
    AtomicBoolean toolResponseReceived = new AtomicBoolean(false);
    Promise<Void> testComplete = Promise.promise();

    final int EXPECTED_LOG_COUNT = 5;

    // Set up notification handler for logging messages
    getClient().addNotificationHandler("notifications/message", notification -> {
      LoggingMessageNotification loggingNotification = new LoggingMessageNotification(notification.toJson());
      logNotifications.add(loggingNotification);

      if (toolResponseReceived.get() && logNotifications.size() >= EXPECTED_LOG_COUNT) {
        testComplete.tryComplete();
      }
    });

    ClientSession session = createSession().await(10, TimeUnit.SECONDS);

    // Send tool call request
    CallToolResult result = (CallToolResult) getClient().sendRequest(
        new CallToolRequest(new JsonObject()
          .put("name", "test-logging")
          .put("arguments", new JsonObject())),
        session
      )
      .expecting(r -> r instanceof CallToolResult)
      .onSuccess(r -> {
        toolResponseReceived.set(true);
        if (logNotifications.size() >= EXPECTED_LOG_COUNT) {
          testComplete.tryComplete();
        }
      })
      .await(10, TimeUnit.SECONDS);

    // Wait for all notifications
    testComplete.future().await(10, TimeUnit.SECONDS);

    context.assertNotNull(result, "Should have tool response");

    JsonObject content = result.getStructuredContent();
    context.assertEquals(5, content.getInteger("logCount"), "Tool should have logged 5 messages");

    context.assertFalse(logNotifications.isEmpty(), "Should have received log notifications, got: " + logNotifications.size());
    context.assertEquals(EXPECTED_LOG_COUNT, logNotifications.size(), "Should have received exactly " + EXPECTED_LOG_COUNT + " log notifications");

    LoggingMessageNotification firstNotification = logNotifications.get(0);
    context.assertEquals(LoggingLevel.DEBUG, firstNotification.getLevel(), "First notification should be DEBUG level");
    context.assertEquals("test-logger", firstNotification.getLogger(), "First notification should have correct logger name");
    context.assertEquals("debug message", firstNotification.getData(), "First notification should have correct message");

    // Verify structured data log
    LoggingMessageNotification structuredNotification = logNotifications.get(4);
    context.assertEquals(LoggingLevel.INFO, structuredNotification.getLevel(), "Structured notification should be INFO level");
    context.assertEquals("structured", structuredNotification.getLogger(), "Structured notification should have correct logger name");
    context.assertTrue(structuredNotification.getData() instanceof JsonObject, "Structured notification should have JsonObject data");
    JsonObject structuredData = (JsonObject) structuredNotification.getData();
    context.assertEquals("value", structuredData.getString("key"));
    context.assertEquals(42, structuredData.getInteger("count"));
  }

  @Test
  public void testDefaultLogLevel(TestContext context) throws Throwable {
    ToolServerFeature toolFeature = new ToolServerFeature();

    toolFeature.addStructuredTool(
      "test-log-level",
      Schemas.objectSchema(),
      Schemas.objectSchema()
        .requiredProperty("initialLevel", Schemas.stringSchema())
        .requiredProperty("changedLevel", Schemas.stringSchema()),
      args -> {
        Context ctx = Vertx.currentContext();
        ServerSession session = ServerSession.fromContext(ctx);

        if (session == null) {
          return Future.failedFuture("No session");
        }

        LoggingLevel initialLevel = session.getLoggingLevel();

        session.setLoggingLevel(LoggingLevel.ERROR);
        LoggingLevel changedLevel = session.getLoggingLevel();

        return Future.succeededFuture(new JsonObject()
          .put("initialLevel", initialLevel.getValue())
          .put("changedLevel", changedLevel.getValue()));
      }
    );

    server.addServerFeature(toolFeature);

    ClientSession session = createSession().await(10, TimeUnit.SECONDS);

    CallToolResult result = (CallToolResult) getClient().sendRequest(
        new CallToolRequest(new JsonObject()
          .put("name", "test-log-level")
          .put("arguments", new JsonObject())),
        session
      )
      .expecting(r -> r instanceof CallToolResult)
      .await(10, TimeUnit.SECONDS);

    context.assertNotNull(result, "Should succeed");
    JsonObject structuredContent = result.getStructuredContent();

    context.assertEquals("info", structuredContent.getString("initialLevel"));
    context.assertEquals("error", structuredContent.getString("changedLevel"));
  }
}
