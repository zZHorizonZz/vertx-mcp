package io.vertx.tests.server;

import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.mcp.common.LogLevel;
import io.vertx.mcp.common.request.InitializeRequest;
import io.vertx.mcp.common.request.SetLevelRequest;
import io.vertx.mcp.common.rpc.JsonError;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.server.feature.LoggingServerFeature;
import io.vertx.mcp.server.transport.http.StreamableHttpServerTransport;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

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
  public void testSetLevelDebug(TestContext context) throws Throwable {
    String sessionId = initializeSession(context);

    JsonResponse response = sendRequest(HttpMethod.POST, new SetLevelRequest().setLevel("debug"), sessionId)
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .expecting(JsonResponse::isSuccess)
      .await(10, TimeUnit.SECONDS);

    context.assertNotNull(response.getResult(), "Should have result");
  }

  @Test
  public void testSetLevelInfo(TestContext context) throws Throwable {
    String sessionId = initializeSession(context);

    sendRequest(HttpMethod.POST, new SetLevelRequest().setLevel("info"), sessionId)
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .expecting(JsonResponse::isSuccess)
      .await(10, TimeUnit.SECONDS);
  }

  @Test
  public void testSetLevelWarning(TestContext context) throws Throwable {
    String sessionId = initializeSession(context);

    sendRequest(HttpMethod.POST, new SetLevelRequest().setLevel("warning"), sessionId)
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .expecting(JsonResponse::isSuccess)
      .await(10, TimeUnit.SECONDS);
  }

  @Test
  public void testSetLevelError(TestContext context) throws Throwable {
    String sessionId = initializeSession(context);

    sendRequest(HttpMethod.POST, new SetLevelRequest().setLevel("error"), sessionId)
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .expecting(JsonResponse::isSuccess)
      .await(10, TimeUnit.SECONDS);
  }

  @Test
  public void testSetLevelCritical(TestContext context) throws Throwable {
    String sessionId = initializeSession(context);

    sendRequest(HttpMethod.POST, new SetLevelRequest().setLevel("critical"), sessionId)
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .expecting(JsonResponse::isSuccess)
      .await(10, TimeUnit.SECONDS);
  }

  @Test
  public void testSetLevelAlert(TestContext context) throws Throwable {
    String sessionId = initializeSession(context);

    sendRequest(HttpMethod.POST, new SetLevelRequest().setLevel("alert"), sessionId)
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .expecting(JsonResponse::isSuccess)
      .await(10, TimeUnit.SECONDS);
  }

  @Test
  public void testSetLevelEmergency(TestContext context) throws Throwable {
    String sessionId = initializeSession(context);

    sendRequest(HttpMethod.POST, new SetLevelRequest().setLevel("emergency"), sessionId)
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .expecting(JsonResponse::isSuccess)
      .await(10, TimeUnit.SECONDS);
  }

  @Test
  public void testSetLevelNotice(TestContext context) throws Throwable {
    String sessionId = initializeSession(context);

    sendRequest(HttpMethod.POST, new SetLevelRequest().setLevel("notice"), sessionId)
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .expecting(JsonResponse::isSuccess)
      .await(10, TimeUnit.SECONDS);
  }

  @Test
  public void testSetLevelInvalidLevel(TestContext context) throws Throwable {
    String sessionId = initializeSession(context);

    JsonResponse response = sendRequest(HttpMethod.POST, new SetLevelRequest().setLevel("invalid"), sessionId)
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .await(10, TimeUnit.SECONDS);

    context.assertNotNull(response.getError(), "Should have error");
    context.assertEquals(JsonError.INVALID_PARAMS, response.getError().getCode(), "Should be invalid params");
    context.assertTrue(response.getError().getMessage().contains("Invalid log level"));
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
    JsonResponse response = sendRequest(HttpMethod.POST, new SetLevelRequest().setLevel("info"))
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

    sendRequest(HttpMethod.POST, new SetLevelRequest().setLevel("debug"), sessionId)
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .expecting(JsonResponse::isSuccess)
      .await(10, TimeUnit.SECONDS);

    sendRequest(HttpMethod.POST, new SetLevelRequest().setLevel("error"), sessionId)
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
  public void testLogConvenienceMethods(TestContext context) {
    feature.init(vertx);

    // These should not throw exceptions even without active sessions
    feature.debug("test-logger", "debug message");
    feature.info("test-logger", "info message");
    feature.warning("test-logger", "warning message");
    feature.error("test-logger", "error message");

    // Log with structured data
    feature.log("info", "structured", new JsonObject()
      .put("key", "value")
      .put("count", 42));

    // Log without logger name
    feature.log("info", "simple message");
  }

  @Test
  public void testDefaultLogLevel(TestContext context) {
    feature.init(vertx);

    // Default should be INFO
    context.assertEquals(LogLevel.INFO, feature.getLogLevel("nonexistent-session"));

    // Can change default
    feature.setDefaultLogLevel(LogLevel.ERROR);
    context.assertEquals(LogLevel.ERROR, feature.getLogLevel("nonexistent-session"));
  }
}
