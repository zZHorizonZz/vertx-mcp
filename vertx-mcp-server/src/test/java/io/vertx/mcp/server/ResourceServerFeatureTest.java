package io.vertx.mcp.server;

import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.mcp.common.request.ListResourceTemplatesRequest;
import io.vertx.mcp.common.request.ListResourcesRequest;
import io.vertx.mcp.common.request.ReadResourceRequest;
import io.vertx.mcp.common.resources.TextResourceContent;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.server.impl.ResourceServerFeature;
import org.junit.Test;

public class ResourceServerFeatureTest extends HttpTransportTestBase {

  @Test
  public void testListResourcesEmpty(TestContext context) {
    ModelContextProtocolServer server = ModelContextProtocolServer.create();
    ResourceServerFeature resourceFeature = new ResourceServerFeature();
    server.serverFeatures(resourceFeature);

    startServer(context, server);

    Async async = context.async();
    JsonRequest request = new ListResourcesRequest().toRequest(1);

    sendRequest(HttpMethod.POST, request.toJson().toBuffer())
      .compose(resp -> resp.body())
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNull(response.getError(), "Should succeed");
        JsonObject result = (JsonObject) response.getResult();
        context.assertNotNull(result, "Should have result");

        JsonArray resources = result.getJsonArray("resources");
        context.assertNotNull(resources, "Should have resources array");
        context.assertEquals(0, resources.size(), "Should be empty");

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testListResourcesWithStaticResources(TestContext context) {
    ModelContextProtocolServer server = ModelContextProtocolServer.create();
    ResourceServerFeature resourceFeature = new ResourceServerFeature();

    // Add static resources
    resourceFeature.addStaticResource("test-resource-1", () ->
      Future.succeededFuture(new TextResourceContent()
        .setUri("resource://test-resource-1")
        .setName("test-resource-1")
        .setText("Test content 1"))
    );

    resourceFeature.addStaticResource("test-resource-2", () ->
      Future.succeededFuture(new TextResourceContent()
        .setUri("resource://test-resource-2")
        .setName("test-resource-2")
        .setText("Test content 2"))
    );

    server.serverFeatures(resourceFeature);
    startServer(context, server);

    Async async = context.async();
    JsonRequest request = new ListResourcesRequest().toRequest(1);

    sendRequest(HttpMethod.POST, request.toJson().toBuffer())
      .compose(resp -> resp.body())
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNull(response.getError(), "Should succeed");
        JsonObject result = (JsonObject) response.getResult();

        JsonArray resources = result.getJsonArray("resources");
        context.assertNotNull(resources, "Should have resources array");
        context.assertEquals(2, resources.size(), "Should have 2 resources");

        // Verify resource structure
        JsonObject resource1 = resources.getJsonObject(0);
        context.assertEquals("resource://test-resource-1", resource1.getString("uri"));
        context.assertEquals("test-resource-1", resource1.getString("name"));

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testReadStaticResource(TestContext context) {
    ModelContextProtocolServer server = ModelContextProtocolServer.create();
    ResourceServerFeature resourceFeature = new ResourceServerFeature();

    String testContent = "This is test content";
    resourceFeature.addStaticResource("test-resource", () ->
      Future.succeededFuture(new TextResourceContent()
        .setUri("resource://test-resource")
        .setName("test-resource")
        .setTitle("Test Resource")
        .setDescription("A test resource")
        .setMimeType("text/plain")
        .setText(testContent))
    );

    server.serverFeatures(resourceFeature);
    startServer(context, server);

    Async async = context.async();

    JsonObject params = new JsonObject()
      .put("uri", "resource://test-resource");
    JsonRequest request = new ReadResourceRequest(params).toRequest(1);

    sendRequest(HttpMethod.POST, request.toJson().toBuffer())
      .compose(resp -> resp.body())
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNull(response.getError(), "Should succeed");
        JsonObject result = (JsonObject) response.getResult();

        JsonArray contents = result.getJsonArray("contents");
        context.assertNotNull(contents, "Should have contents array");
        context.assertEquals(1, contents.size(), "Should have 1 content item");

        JsonObject content = contents.getJsonObject(0);
        context.assertEquals("resource://test-resource", content.getString("uri"));
        context.assertEquals("test-resource", content.getString("name"));
        context.assertEquals("Test Resource", content.getString("title"));
        context.assertEquals("A test resource", content.getString("description"));
        context.assertEquals("text/plain", content.getString("mimeType"));
        context.assertEquals(testContent, content.getString("text"));

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testReadResourceNotFound(TestContext context) {
    ModelContextProtocolServer server = ModelContextProtocolServer.create();
    ResourceServerFeature resourceFeature = new ResourceServerFeature();
    server.serverFeatures(resourceFeature);

    startServer(context, server);

    Async async = context.async();

    JsonObject params = new JsonObject()
      .put("uri", "resource://nonexistent");
    JsonRequest request = new ReadResourceRequest(params).toRequest(1);

    sendRequest(HttpMethod.POST, request.toJson().toBuffer())
      .compose(resp -> resp.body())
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNotNull(response.getError(), "Should have error");
        context.assertEquals(-32602, response.getError().getCode(), "Should be invalid params");
        context.assertTrue(response.getError().getMessage().contains("not found"));

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testReadResourceMissingUri(TestContext context) {
    ModelContextProtocolServer server = ModelContextProtocolServer.create();
    ResourceServerFeature resourceFeature = new ResourceServerFeature();
    server.serverFeatures(resourceFeature);

    startServer(context, server);

    Async async = context.async();

    JsonObject params = new JsonObject(); // Missing uri parameter
    JsonRequest request = JsonRequest.createRequest("resources/read", params, 1);

    sendRequest(HttpMethod.POST, request.toJson().toBuffer())
      .compose(resp -> resp.body())
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNotNull(response.getError(), "Should have error");
        context.assertEquals(-32602, response.getError().getCode(), "Should be invalid params");
        context.assertTrue(response.getError().getMessage().contains("Missing 'uri'"));

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testListResourceTemplates(TestContext context) {
    ModelContextProtocolServer server = ModelContextProtocolServer.create();
    ResourceServerFeature resourceFeature = new ResourceServerFeature();
    server.serverFeatures(resourceFeature);

    startServer(context, server);

    Async async = context.async();
    JsonRequest request = new ListResourceTemplatesRequest().toRequest(1);

    sendRequest(HttpMethod.POST, request.toJson().toBuffer())
      .compose(resp -> resp.body())
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNull(response.getError(), "Should succeed");
        JsonObject result = (JsonObject) response.getResult();
        context.assertNotNull(result, "Should have result");

        JsonArray templates = result.getJsonArray("resourceTemplates");
        context.assertNotNull(templates, "Should have resourceTemplates array");

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testResourceHandlerFailure(TestContext context) {
    ModelContextProtocolServer server = ModelContextProtocolServer.create();
    ResourceServerFeature resourceFeature = new ResourceServerFeature();

    // Add a resource that fails
    resourceFeature.addStaticResource("failing-resource", () ->
      Future.failedFuture("Resource generation failed")
    );

    server.serverFeatures(resourceFeature);
    startServer(context, server);

    Async async = context.async();

    JsonObject params = new JsonObject()
      .put("uri", "resource://failing-resource");
    JsonRequest request = new ReadResourceRequest(params).toRequest(1);

    sendRequest(HttpMethod.POST, request.toJson().toBuffer())
      .compose(resp -> resp.body())
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNotNull(response.getError(), "Should have error");
        context.assertEquals(-32603, response.getError().getCode(), "Should be internal error");

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testUnsupportedResourceMethod(TestContext context) {
    ModelContextProtocolServer server = ModelContextProtocolServer.create();
    ResourceServerFeature resourceFeature = new ResourceServerFeature();
    server.serverFeatures(resourceFeature);

    startServer(context, server);

    Async async = context.async();

    JsonRequest request = JsonRequest.createRequest("resources/unsupported", new JsonObject(), 1);

    sendRequest(HttpMethod.POST, request.toJson().toBuffer())
      .compose(resp -> resp.body())
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNotNull(response.getError(), "Should have error");
        context.assertEquals(-32601, response.getError().getCode(), "Should be method not found");

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testMultipleStaticResources(TestContext context) {
    ModelContextProtocolServer server = ModelContextProtocolServer.create();
    ResourceServerFeature resourceFeature = new ResourceServerFeature();

    // Add multiple resources
    for (int i = 0; i < 5; i++) {
      final int index = i;
      resourceFeature.addStaticResource("resource-" + i, () ->
        Future.succeededFuture(new TextResourceContent()
          .setUri("resource://resource-" + index)
          .setName("resource-" + index)
          .setText("Content " + index))
      );
    }

    server.serverFeatures(resourceFeature);
    startServer(context, server);

    Async async = context.async();
    JsonRequest request = new ListResourcesRequest().toRequest(1);

    sendRequest(HttpMethod.POST, request.toJson().toBuffer())
      .compose(resp -> resp.body())
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNull(response.getError(), "Should succeed");
        JsonObject result = (JsonObject) response.getResult();

        JsonArray resources = result.getJsonArray("resources");
        context.assertEquals(5, resources.size(), "Should have 5 resources");

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }
}
