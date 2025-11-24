package io.vertx.tests.server;

import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.mcp.common.request.InitializeRequest;
import io.vertx.mcp.common.request.PingRequest;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.server.ModelContextProtocolServer;
import io.vertx.mcp.server.ServerOptions;
import io.vertx.mcp.server.feature.ResourceServerFeature;
import org.junit.Test;

public class ProtocolServerFeatureTest extends HttpTransportTestBase {

  @Test
  public void testInitializeReturnsServerInfo(TestContext context) {
    Async async = context.async();

    ServerOptions options = new ServerOptions()
      .setServerName("test-server")
      .setServerVersion("1.0.0");

    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx, options);
    startServer(context, server);

    sendRequest(HttpMethod.POST, new InitializeRequest()).compose(HttpClientResponse::body).onComplete(context.asyncAssertSuccess(body -> {
      JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

      context.assertNull(response.getError(), "Initialize should succeed");
      JsonObject result = (JsonObject) response.getResult();
      context.assertNotNull(result, "Should have result");

      JsonObject serverInfo = result.getJsonObject("serverInfo");
      context.assertNotNull(serverInfo, "Should have serverInfo");
      context.assertEquals("test-server", serverInfo.getString("name"));
      context.assertEquals("1.0.0", serverInfo.getString("version"));

      JsonObject capabilities = result.getJsonObject("capabilities");
      context.assertNotNull(capabilities, "Should have capabilities");

      async.complete();
    }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testInitializeWithResourcesCapability(TestContext context) {
    Async async = context.async();

    ServerOptions options = new ServerOptions()
      .setSessionsEnabled(true);

    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx, options);
    server.addServerFeature(new ResourceServerFeature());

    startServer(context, server);

    sendRequest(HttpMethod.POST, new InitializeRequest()).compose(HttpClientResponse::body).onComplete(context.asyncAssertSuccess(body -> {
      JsonResponse response = JsonResponse.fromJson(body.toJsonObject());
      JsonObject capabilities = ((JsonObject) response.getResult()).getJsonObject("capabilities");

      context.assertTrue(capabilities.containsKey("resources"), "Should have resources capability");

      JsonObject resources = capabilities.getJsonObject("resources");
      context.assertTrue(resources.getBoolean("subscribe"), "Should support subscribe when sessions enabled");

      async.complete();
    }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testInitializeWithoutSessionsNoSubscribe(TestContext context) {
    Async async = context.async();

    ServerOptions options = new ServerOptions()
      .setStreamingEnabled(false)
      .setSessionsEnabled(false);

    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx, options);
    server.addServerFeature(new ResourceServerFeature());

    startServer(context, server);

    sendRequest(HttpMethod.POST, new InitializeRequest()).compose(HttpClientResponse::body).onComplete(context.asyncAssertSuccess(body -> {
      JsonResponse response = JsonResponse.fromJson(body.toJsonObject());
      JsonObject capabilities = ((JsonObject) response.getResult()).getJsonObject("capabilities");

      if (capabilities.containsKey("resources")) {
        JsonObject resources = capabilities.getJsonObject("resources");
        context.assertFalse(resources.containsKey("subscribe") && resources.getBoolean("subscribe"),
          "Should not support subscribe when sessions disabled");
      }

      async.complete();
    }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testPing(TestContext context) {
    Async async = context.async();

    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx);
    startServer(context, server);

    sendRequest(HttpMethod.POST, new PingRequest()).compose(HttpClientResponse::body).onComplete(context.asyncAssertSuccess(body -> {
      JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

      context.assertNull(response.getError(), "Ping should succeed");
      context.assertNotNull(response.getResult(), "Should have result");
      context.assertTrue(((JsonObject) response.getResult()).isEmpty(), "Ping result should be empty");

      async.complete();
    }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testUnknownMethod(TestContext context) {
    Async async = context.async();

    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx);
    startServer(context, server);

    sendRequest(HttpMethod.POST, JsonRequest.createRequest("unknown/method", new JsonObject(), 1)).compose(HttpClientResponse::body).onComplete(context.asyncAssertSuccess(body -> {
      JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

      context.assertNotNull(response.getError(), "Should have error");
      context.assertEquals(-32601, response.getError().getCode(), "Should be method not found");
      context.assertTrue(response.getError().getMessage().contains("unknown/method"));

      async.complete();
    }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testInvalidJsonRpcRequest(TestContext context) {
    Async async = context.async();

    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx);
    startServer(context, server);

    JsonObject request = new JsonObject().put("invalid", "request");

    sendRequest(HttpMethod.POST, request.toBuffer()).compose(HttpClientResponse::body).onComplete(context.asyncAssertSuccess(body -> {
      JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

      context.assertNotNull(response.getError(), "Should have error");
      context.assertEquals(-32600, response.getError().getCode(), "Should be invalid request");

      async.complete();
    }));

    async.awaitSuccess(10_000);
  }
}
