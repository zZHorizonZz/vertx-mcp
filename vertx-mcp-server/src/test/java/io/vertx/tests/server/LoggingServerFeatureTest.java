package io.vertx.tests.server;

import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.mcp.common.request.InitializeRequest;
import io.vertx.mcp.common.request.SetLevelRequest;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.server.feature.LoggingServerFeature;
import io.vertx.mcp.server.transport.http.StreamableHttpServerTransport;
import org.junit.Test;

public class LoggingServerFeatureTest extends ServerFeatureTestBase<LoggingServerFeature> {

  @Override
  protected LoggingServerFeature createFeature() {
    return new LoggingServerFeature();
  }

  private String initializeSession(TestContext context) {
    try {
      return sendRequest(HttpMethod.POST, new InitializeRequest())
        .compose(resp -> resp.body().map(body -> resp.getHeader(StreamableHttpServerTransport.MCP_SESSION_ID_HEADER)))
        .toCompletionStage()
        .toCompletableFuture()
        .get();
    } catch (Exception e) {
      context.fail(e);
      return null;
    }
  }

  @Test
  public void testSetLevelDebug(TestContext context) {
    Async async = context.async();
    String sessionId = initializeSession(context);

    sendRequest(HttpMethod.POST, new SetLevelRequest().setLevel("debug"), sessionId)
      .compose(HttpClientResponse::body)
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNull(response.getError(), "Should succeed");
        context.assertNotNull(response.getResult(), "Should have result");

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testSetLevelInfo(TestContext context) {
    Async async = context.async();
    String sessionId = initializeSession(context);

    sendRequest(HttpMethod.POST, new SetLevelRequest().setLevel("info"), sessionId)
      .compose(HttpClientResponse::body)
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());
        context.assertNull(response.getError(), "Should succeed");
        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testSetLevelWarning(TestContext context) {
    Async async = context.async();
    String sessionId = initializeSession(context);

    sendRequest(HttpMethod.POST, new SetLevelRequest().setLevel("warning"), sessionId)
      .compose(HttpClientResponse::body)
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());
        context.assertNull(response.getError(), "Should succeed");
        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testSetLevelError(TestContext context) {
    Async async = context.async();
    String sessionId = initializeSession(context);

    sendRequest(HttpMethod.POST, new SetLevelRequest().setLevel("error"), sessionId)
      .compose(HttpClientResponse::body)
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());
        context.assertNull(response.getError(), "Should succeed");
        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testSetLevelCritical(TestContext context) {
    Async async = context.async();
    String sessionId = initializeSession(context);

    sendRequest(HttpMethod.POST, new SetLevelRequest().setLevel("critical"), sessionId)
      .compose(HttpClientResponse::body)
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());
        context.assertNull(response.getError(), "Should succeed");
        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testSetLevelAlert(TestContext context) {
    Async async = context.async();
    String sessionId = initializeSession(context);

    sendRequest(HttpMethod.POST, new SetLevelRequest().setLevel("alert"), sessionId)
      .compose(HttpClientResponse::body)
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());
        context.assertNull(response.getError(), "Should succeed");
        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testSetLevelEmergency(TestContext context) {
    Async async = context.async();
    String sessionId = initializeSession(context);

    sendRequest(HttpMethod.POST, new SetLevelRequest().setLevel("emergency"), sessionId)
      .compose(HttpClientResponse::body)
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());
        context.assertNull(response.getError(), "Should succeed");
        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testSetLevelNotice(TestContext context) {
    Async async = context.async();
    String sessionId = initializeSession(context);

    sendRequest(HttpMethod.POST, new SetLevelRequest().setLevel("notice"), sessionId)
      .compose(HttpClientResponse::body)
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());
        context.assertNull(response.getError(), "Should succeed");
        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testSetLevelInvalidLevel(TestContext context) {
    Async async = context.async();
    String sessionId = initializeSession(context);

    sendRequest(HttpMethod.POST, new SetLevelRequest().setLevel("invalid"), sessionId)
      .compose(HttpClientResponse::body)
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNotNull(response.getError(), "Should have error");
        context.assertEquals(-32602, response.getError().getCode(), "Should be invalid params");
        context.assertTrue(response.getError().getMessage().contains("Invalid log level"));

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testSetLevelMissingLevel(TestContext context) {
    Async async = context.async();
    String sessionId = initializeSession(context);

    sendRequest(HttpMethod.POST, new SetLevelRequest(), sessionId)
      .compose(HttpClientResponse::body)
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNotNull(response.getError(), "Should have error");
        context.assertEquals(-32602, response.getError().getCode(), "Should be invalid params");
        context.assertTrue(response.getError().getMessage().contains("level"));

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testSetLevelMissingParams(TestContext context) {
    Async async = context.async();
    String sessionId = initializeSession(context);

    sendRequest(HttpMethod.POST, JsonRequest.createRequest("logging/setLevel", null, 1), sessionId)
      .compose(HttpClientResponse::body)
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNotNull(response.getError(), "Should have error");
        context.assertEquals(-32602, response.getError().getCode(), "Should be invalid params");

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testSetLevelRequiresSession(TestContext context) {
    Async async = context.async();

    // Send request without session
    sendRequest(HttpMethod.POST, new SetLevelRequest().setLevel("info"))
      .compose(HttpClientResponse::body)
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNotNull(response.getError(), "Should have error");
        context.assertEquals(-32600, response.getError().getCode(), "Should be invalid request");
        context.assertTrue(response.getError().getMessage().contains("session"));

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testMultipleLevelChanges(TestContext context) {
    Async async = context.async();
    String sessionId = initializeSession(context);

    // First set to debug
    sendRequest(HttpMethod.POST, new SetLevelRequest().setLevel("debug"), sessionId)
      .compose(HttpClientResponse::body)
      .compose(body1 -> {
        JsonResponse response1 = JsonResponse.fromJson(body1.toJsonObject());
        context.assertNull(response1.getError(), "First setLevel should succeed");

        // Then change to error
        return sendRequest(HttpMethod.POST, new SetLevelRequest().setLevel("error"), sessionId)
          .compose(HttpClientResponse::body);
      })
      .onComplete(context.asyncAssertSuccess(body2 -> {
        JsonResponse response2 = JsonResponse.fromJson(body2.toJsonObject());
        context.assertNull(response2.getError(), "Second setLevel should succeed");

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testCapabilities(TestContext context) {
    // Verify the feature exposes the correct capability
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

    // Default should be "info"
    context.assertEquals("info", feature.getLogLevel("nonexistent-session"));

    // Can change default
    feature.setDefaultLogLevel("error");
    context.assertEquals("error", feature.getLogLevel("nonexistent-session"));
  }
}
