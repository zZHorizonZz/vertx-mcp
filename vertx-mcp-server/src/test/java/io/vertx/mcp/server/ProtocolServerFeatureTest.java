package io.vertx.mcp.server;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.server.impl.ResourceServerFeature;
import org.junit.Test;

public class ProtocolServerFeatureTest extends HttpTransportTestBase {

  @Test
  public void testInitializeReturnsServerInfo(TestContext context) {
    ServerOptions options = new ServerOptions()
      .setServerName("test-server")
      .setServerVersion("1.0.0");

    ModelContextProtocolServer server = ModelContextProtocolServer.create(options);
    startServer(context, server);

    HttpClient client = vertx.createHttpClient(new HttpClientOptions());
    Async async = context.async();

    JsonRequest initRequest = JsonRequest.createRequest("initialize", new JsonObject(), 1);

    client.request(HttpMethod.POST, port, "localhost", "/")
      .compose(req -> {
        req.putHeader("Content-Type", "application/json");
        return req.send(initRequest.toJson().toBuffer())
          .compose(resp -> resp.body());
      })
      .onComplete(context.asyncAssertSuccess(body -> {
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
    ServerOptions options = new ServerOptions()
      .setSessionsEnabled(true);

    ModelContextProtocolServer server = ModelContextProtocolServer.create(options);
    server.serverFeatures(new ResourceServerFeature());

    startServer(context, server);

    HttpClient client = vertx.createHttpClient(new HttpClientOptions());
    Async async = context.async();

    JsonRequest initRequest = JsonRequest.createRequest("initialize", new JsonObject(), 1);

    client.request(HttpMethod.POST, port, "localhost", "/")
      .compose(req -> {
        req.putHeader("Content-Type", "application/json");
        return req.send(initRequest.toJson().toBuffer())
          .compose(resp -> resp.body());
      })
      .onComplete(context.asyncAssertSuccess(body -> {
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
    ServerOptions options = new ServerOptions()
      .setSessionsEnabled(false);

    ModelContextProtocolServer server = ModelContextProtocolServer.create(options);
    server.serverFeatures(new ResourceServerFeature());

    startServer(context, server);

    HttpClient client = vertx.createHttpClient(new HttpClientOptions());
    Async async = context.async();

    JsonRequest initRequest = JsonRequest.createRequest("initialize", new JsonObject(), 1);

    client.request(HttpMethod.POST, port, "localhost", "/")
      .compose(req -> {
        req.putHeader("Content-Type", "application/json");
        return req.send(initRequest.toJson().toBuffer())
          .compose(resp -> resp.body());
      })
      .onComplete(context.asyncAssertSuccess(body -> {
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
    ModelContextProtocolServer server = ModelContextProtocolServer.create();
    startServer(context, server);

    HttpClient client = vertx.createHttpClient(new HttpClientOptions());
    Async async = context.async();

    JsonRequest pingRequest = JsonRequest.createRequest("ping", new JsonObject(), 1);

    client.request(HttpMethod.POST, port, "localhost", "/")
      .compose(req -> {
        req.putHeader("Content-Type", "application/json");
        return req.send(pingRequest.toJson().toBuffer())
          .compose(resp -> resp.body());
      })
      .onComplete(context.asyncAssertSuccess(body -> {
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
    ModelContextProtocolServer server = ModelContextProtocolServer.create();
    startServer(context, server);

    HttpClient client = vertx.createHttpClient(new HttpClientOptions());
    Async async = context.async();

    JsonRequest unknownRequest = JsonRequest.createRequest("unknown/method", new JsonObject(), 1);

    client.request(HttpMethod.POST, port, "localhost", "/")
      .compose(req -> {
        req.putHeader("Content-Type", "application/json");
        return req.send(unknownRequest.toJson().toBuffer())
          .compose(resp -> resp.body());
      })
      .onComplete(context.asyncAssertSuccess(body -> {
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
    ModelContextProtocolServer server = ModelContextProtocolServer.create();
    startServer(context, server);

    HttpClient client = vertx.createHttpClient(new HttpClientOptions());
    Async async = context.async();

    // Invalid JSON-RPC (missing required fields)
    JsonObject invalidRequest = new JsonObject()
      .put("invalid", "request");

    client.request(HttpMethod.POST, port, "localhost", "/")
      .compose(req -> {
        req.putHeader("Content-Type", "application/json");
        return req.send(invalidRequest.toBuffer())
          .compose(resp -> resp.body());
      })
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNotNull(response.getError(), "Should have error");
        context.assertEquals(-32600, response.getError().getCode(), "Should be invalid request");

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }
}
