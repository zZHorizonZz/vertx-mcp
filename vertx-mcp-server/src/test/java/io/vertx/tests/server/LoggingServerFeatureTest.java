package io.vertx.tests.server;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.json.schema.common.dsl.Schemas;
import io.vertx.mcp.common.LoggingLevel;
import io.vertx.mcp.common.notification.LoggingMessageNotification;
import io.vertx.mcp.common.request.CallToolRequest;
import io.vertx.mcp.common.request.InitializeRequest;
import io.vertx.mcp.common.request.SetLevelRequest;
import io.vertx.mcp.common.rpc.JsonError;
import io.vertx.mcp.common.rpc.JsonNotification;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.server.ServerSession;
import io.vertx.mcp.server.feature.LoggingServerFeature;
import io.vertx.mcp.server.feature.ToolServerFeature;
import io.vertx.mcp.server.transport.http.StreamableHttpServerTransport;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class LoggingServerFeatureTest extends ServerFeatureTestBase<LoggingServerFeature> {

  @Override
  protected LoggingServerFeature createFeature() {
    return new LoggingServerFeature();
  }

  private String initializeSession(TestContext context) throws Throwable {
    return sendRequest(HttpMethod.POST, new InitializeRequest())
      .compose(resp -> resp.body().map(body -> resp.getHeader(StreamableHttpServerTransport.MCP_SESSION_ID_HEADER)))
      .await(10, TimeUnit.SECONDS);
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

    String sessionId = initializeSession(context);

    for (LoggingLevel level : levels) {
      JsonResponse response = sendRequest(
        HttpMethod.POST,
        new SetLevelRequest().setLevel(level),
        sessionId
      )
        .compose(HttpClientResponse::body)
        .map(body -> JsonResponse.fromJson(body.toJsonObject()))
        .expecting(JsonResponse::isSuccess)
        .await(10, TimeUnit.SECONDS);

      context.assertNotNull(response.getResult(),
        "Should have result for level: " + level.getValue());
    }
  }

  @Test
  public void testSetLevelMissingLevel(TestContext context) throws Throwable {
    String sessionId = initializeSession(context);

    JsonResponse response = sendRequest(HttpMethod.POST, new SetLevelRequest(), sessionId)
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .await(10, TimeUnit.SECONDS);

    context.assertNotNull(response.getError(), "Should have error");
    context.assertEquals(JsonError.INVALID_PARAMS, response.getError().getCode(), "Should be invalid params");
    context.assertTrue(response.getError().getMessage().contains("level"));
  }

  @Test
  public void testSetLevelMissingParams(TestContext context) throws Throwable {
    String sessionId = initializeSession(context);

    JsonResponse response = sendRequest(HttpMethod.POST, JsonRequest.createRequest("logging/setLevel", null, 1), sessionId)
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .await(10, TimeUnit.SECONDS);

    context.assertNotNull(response.getError(), "Should have error");
    context.assertEquals(JsonError.INVALID_PARAMS, response.getError().getCode(), "Should be invalid params");
  }

  @Test
  public void testSetLevelRequiresSession(TestContext context) throws Throwable {
    JsonResponse response = sendRequest(HttpMethod.POST, new SetLevelRequest().setLevel(LoggingLevel.INFO))
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .await(10, TimeUnit.SECONDS);

    context.assertNotNull(response.getError(), "Should have error");
    context.assertEquals(JsonError.INVALID_REQUEST, response.getError().getCode(), "Should be invalid request");
    context.assertTrue(response.getError().getMessage().contains("session"));
  }

  @Test
  public void testMultipleLevelChanges(TestContext context) throws Throwable {
    String sessionId = initializeSession(context);

    sendRequest(HttpMethod.POST, new SetLevelRequest().setLevel(LoggingLevel.DEBUG), sessionId)
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .expecting(JsonResponse::isSuccess)
      .await(10, TimeUnit.SECONDS);

    sendRequest(HttpMethod.POST, new SetLevelRequest().setLevel(LoggingLevel.ERROR), sessionId)
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .expecting(JsonResponse::isSuccess)
      .await(10, TimeUnit.SECONDS);
  }

  @Test
  public void testCapabilities(TestContext context) {
    context.assertTrue(feature.getCapabilities().contains("logging/setLevel"));
    context.assertEquals(1, feature.getCapabilities().size());
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

    mcpServer.addServerFeature(toolFeature);

    String sessionId = initializeSession(context);

    HttpClient client = vertx.createHttpClient();
    List<JsonObject> allMessages = new ArrayList<>();
    Promise<Void> testComplete = Promise.promise();

    final int EXPECTED_LOG_COUNT = 5;
    AtomicBoolean toolResponseReceived = new AtomicBoolean(false);
    AtomicInteger logNotificationCount = new AtomicInteger(0);

    client.request(HttpMethod.POST, port, "localhost", "/mcp")
      .compose(req -> {
        req.putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        req.putHeader(HttpHeaders.ACCEPT, "application/json, text/event-stream");
        req.putHeader(StreamableHttpServerTransport.MCP_SESSION_ID_HEADER, sessionId);

        Buffer requestBody = new CallToolRequest(new JsonObject()
          .put("name", "test-logging")
          .put("arguments", new JsonObject())).toRequest(1).toJson().toBuffer();

        return req.send(requestBody);
      })
      .onSuccess(resp -> {
        StringBuilder accumulated = new StringBuilder();

        resp.handler(chunk -> {
          accumulated.append(chunk.toString());
          String data = accumulated.toString();

          String[] parts = data.split("\n\n");

          for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];

            if (part.startsWith("data: ")) {
              String json = part.substring(6).trim();
              try {
                JsonObject msg = new JsonObject(json);
                allMessages.add(msg);

                if (msg.containsKey("result")) {
                  toolResponseReceived.set(true);
                }

                if ("notifications/message".equals(msg.getString("method"))) {
                  logNotificationCount.incrementAndGet();
                }

                if (toolResponseReceived.get() && logNotificationCount.get() >= EXPECTED_LOG_COUNT) {
                  testComplete.tryComplete();
                }
              } catch (Exception e) {
                // Ignore parse errors
              }
            }
          }

          if (parts.length > 0) {
            accumulated.setLength(0);
            accumulated.append(parts[parts.length - 1]).append("\n\n");
          }
        });

        resp.endHandler(v -> {
          String remaining = accumulated.toString().trim();
          if (remaining.startsWith("data: ")) {
            String json = remaining.substring(6).trim();
            try {
              JsonObject msg = new JsonObject(json);
              allMessages.add(msg);

              if (msg.containsKey("result")) {
                toolResponseReceived.set(true);
              }

              if ("notifications/message".equals(msg.getString("method"))) {
                logNotificationCount.incrementAndGet();
              }

              if (toolResponseReceived.get() && logNotificationCount.get() >= EXPECTED_LOG_COUNT) {
                testComplete.tryComplete();
              }
            } catch (Exception e) {
              // Ignore parse errors
            }
          }

          testComplete.tryComplete();
        });

        resp.exceptionHandler(testComplete::tryFail);
      })
      .onFailure(testComplete::tryFail);

    testComplete.future().await(10, TimeUnit.SECONDS);

    JsonResponse toolResponse = null;
    List<LoggingMessageNotification> logNotifications = new ArrayList<>();

    for (JsonObject msg : allMessages) {
      if (msg.containsKey("result")) {
        toolResponse = JsonResponse.fromJson(msg);
      } else if ("notifications/message".equals(msg.getString("method"))) {
        JsonNotification notification = new JsonNotification(msg);
        LoggingMessageNotification loggingNotification = new LoggingMessageNotification(notification.getNamedParams());
        logNotifications.add(loggingNotification);
      }
    }

    context.assertNotNull(toolResponse, "Should have tool response");

    JsonObject result = (JsonObject) toolResponse.getResult();
    context.assertNotNull(result, "Tool should return result");

    JsonObject content = result.getJsonObject("structuredContent");
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

    mcpServer.addServerFeature(toolFeature);

    String sessionId = initializeSession(context);

    JsonResponse response = sendRequest(
      HttpMethod.POST,
      new CallToolRequest(new JsonObject()
        .put("name", "test-log-level")
        .put("arguments", new JsonObject())),
      sessionId
    )
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .await(10, TimeUnit.SECONDS);

    context.assertNull(response.getError(), "Should succeed");
    JsonObject result = (JsonObject) response.getResult();
    JsonObject structuredContent = result.getJsonObject("structuredContent");

    context.assertEquals("info", structuredContent.getString("initialLevel"));
    context.assertEquals("error", structuredContent.getString("changedLevel"));
  }
}
