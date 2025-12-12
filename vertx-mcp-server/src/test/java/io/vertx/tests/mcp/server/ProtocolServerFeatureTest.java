package io.vertx.tests.mcp.server;

import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.mcp.common.request.InitializeRequest;
import io.vertx.mcp.common.request.PingRequest;
import io.vertx.mcp.common.rpc.JsonError;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.server.ModelContextProtocolServer;
import io.vertx.mcp.server.ServerOptions;
import io.vertx.mcp.server.feature.ProtocolServerFeature;
import io.vertx.mcp.server.feature.ResourceServerFeature;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class ProtocolServerFeatureTest extends HttpTransportTestBase {

  @Test
  public void testInitializeReturnsServerInfo(TestContext context) throws Throwable {
    ServerOptions options = new ServerOptions().setServerName("test-server").setServerVersion("1.0.0");
    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx, options);

    server.addServerFeature(new ProtocolServerFeature());

    startServer(context, server);

    JsonResponse response = sendRequest(HttpMethod.POST, new InitializeRequest())
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .expecting(JsonResponse::isSuccess)
      .await(10, TimeUnit.SECONDS);

    JsonObject result = (JsonObject) response.getResult();
    context.assertNotNull(result, "Should have result");

    JsonObject serverInfo = result.getJsonObject("serverInfo");
    context.assertNotNull(serverInfo, "Should have serverInfo");
    context.assertEquals("test-server", serverInfo.getString("name"));
    context.assertEquals("1.0.0", serverInfo.getString("version"));

    JsonObject capabilities = result.getJsonObject("capabilities");
    context.assertNotNull(capabilities, "Should have capabilities");
  }

  @Test
  public void testInitializeWithResourcesCapability(TestContext context) throws Throwable {
    ServerOptions options = new ServerOptions();
    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx, options);

    server.addServerFeature(new ProtocolServerFeature());
    server.addServerFeature(new ResourceServerFeature());

    startServer(context, server);

    JsonResponse response = sendRequest(HttpMethod.POST, new InitializeRequest())
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .expecting(JsonResponse::isSuccess)
      .await(10, TimeUnit.SECONDS);

    JsonObject capabilities = ((JsonObject) response.getResult()).getJsonObject("capabilities");

    context.assertTrue(capabilities.containsKey("resources"), "Should have resources capability");

    JsonObject resources = capabilities.getJsonObject("resources");
    context.assertTrue(resources.getBoolean("subscribe"), "Should support subscribe when sessions enabled");
  }

  @Test
  public void testInitializeWithoutSessionsNoSubscribe(TestContext context) throws Throwable {
    ServerOptions options = new ServerOptions().setStreamingEnabled(false);
    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx, options);

    server.addServerFeature(new ProtocolServerFeature());
    server.addServerFeature(new ResourceServerFeature());

    startServer(context, server);

    JsonResponse response = sendRequest(HttpMethod.POST, new InitializeRequest())
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .expecting(JsonResponse::isSuccess)
      .await(10, TimeUnit.SECONDS);

    JsonObject capabilities = ((JsonObject) response.getResult()).getJsonObject("capabilities");

    if (capabilities.containsKey("resources")) {
      JsonObject resources = capabilities.getJsonObject("resources");
      context.assertFalse(resources.containsKey("subscribe") && resources.getBoolean("subscribe"), "Should not support subscribe when sessions disabled");
    }
  }

  @Test
  public void testPing(TestContext context) throws Throwable {
    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx);

    server.addServerFeature(new ProtocolServerFeature());

    startServer(context, server);

    JsonResponse response = sendRequest(HttpMethod.POST, new PingRequest())
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .expecting(JsonResponse::isSuccess)
      .await(10, TimeUnit.SECONDS);

    context.assertNotNull(response.getResult(), "Should have result");
    context.assertTrue(((JsonObject) response.getResult()).isEmpty(), "Ping result should be empty");
  }

  @Test
  public void testUnknownMethod(TestContext context) throws Throwable {
    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx);

    server.addServerFeature(new ProtocolServerFeature());

    startServer(context, server);

    JsonResponse response = sendRequest(HttpMethod.POST, JsonRequest.createRequest("unknown/method", new JsonObject(), 1))
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .await(10, TimeUnit.SECONDS);

    context.assertNotNull(response.getError(), "Should have error");
    context.assertEquals(JsonError.METHOD_NOT_FOUND, response.getError().getCode(), "Should be method not found");
    context.assertTrue(response.getError().getMessage().contains("unknown/method"));
  }

  @Test
  public void testInvalidJsonRpcRequest(TestContext context) throws Throwable {
    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx);

    server.addServerFeature(new ProtocolServerFeature());

    startServer(context, server);

    JsonObject request = new JsonObject().put("invalid", "request");

    JsonResponse response = sendRequest(HttpMethod.POST, request.toBuffer())
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .await(10, TimeUnit.SECONDS);

    context.assertNotNull(response.getError(), "Should have error");
    context.assertEquals(JsonError.INVALID_REQUEST, response.getError().getCode(), "Should be invalid request");
  }
}
