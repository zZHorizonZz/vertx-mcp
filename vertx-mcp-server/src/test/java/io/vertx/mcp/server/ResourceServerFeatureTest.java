package io.vertx.mcp.server;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClientResponse;
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

public class ResourceServerFeatureTest extends ServerFeatureTestBase<ResourceServerFeature> {

  @Override
  protected ResourceServerFeature createFeature() {
    return new ResourceServerFeature();
  }

  @Test
  public void testListResourcesEmpty(TestContext context) {
    Async async = context.async();

    sendRequest(HttpMethod.POST, new ListResourcesRequest())
      .compose(HttpClientResponse::body)
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
    feature.addStaticResource("test-resource-1", () ->
      Future.succeededFuture(new TextResourceContent()
        .setUri("resource://test-resource-1")
        .setName("test-resource-1")
        .setText("Test content 1"))
    );

    feature.addStaticResource("test-resource-2", () ->
      Future.succeededFuture(new TextResourceContent()
        .setUri("resource://test-resource-2")
        .setName("test-resource-2")
        .setText("Test content 2"))
    );

    Async async = context.async();

    sendRequest(HttpMethod.POST, new ListResourcesRequest())
      .compose(HttpClientResponse::body)
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
    Async async = context.async();
    String testContent = "This is test content";

    feature.addStaticResource("test-resource", () ->
      Future.succeededFuture(new TextResourceContent()
        .setUri("resource://test-resource")
        .setName("test-resource")
        .setTitle("Test Resource")
        .setDescription("A test resource")
        .setMimeType("text/plain")
        .setText(testContent))
    );

    sendRequest(HttpMethod.POST, new ReadResourceRequest(new JsonObject().put("uri", "resource://test-resource")))
      .compose(HttpClientResponse::body)
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
    Async async = context.async();

    sendRequest(HttpMethod.POST, new ReadResourceRequest(new JsonObject().put("uri", "resource://nonexistent")))
      .compose(HttpClientResponse::body)
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
    Async async = context.async();

    sendRequest(HttpMethod.POST, JsonRequest.createRequest("resources/read", new JsonObject(), 1))
      .compose(HttpClientResponse::body)
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
    Async async = context.async();

    sendRequest(HttpMethod.POST, new ListResourceTemplatesRequest())
      .compose(HttpClientResponse::body)
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
    Async async = context.async();

    // Add a resource that fails
    feature.addStaticResource("failing-resource", () ->
      Future.failedFuture("Resource generation failed")
    );

    sendRequest(HttpMethod.POST, new ReadResourceRequest(new JsonObject().put("uri", "resource://failing-resource")))
      .compose(HttpClientResponse::body)
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
    Async async = context.async();

    sendRequest(HttpMethod.POST, JsonRequest.createRequest("resources/unsupported", new JsonObject(), 1))
      .compose(HttpClientResponse::body)
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
    Async async = context.async();

    // Add multiple resources
    for (int i = 0; i < 5; i++) {
      final int index = i;
      feature.addStaticResource("resource-" + i, () ->
        Future.succeededFuture(new TextResourceContent()
          .setUri("resource://resource-" + index)
          .setName("resource-" + index)
          .setText("Content " + index))
      );
    }

    sendRequest(HttpMethod.POST, new ListResourcesRequest())
      .compose(HttpClientResponse::body)
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

  @Test
  public void testListResourceTemplatesWithDynamicResources(TestContext context) {
    Async async = context.async();

    // Add dynamic resources with templates
    feature.addDynamicResource("resource://user/{id}", params ->
      Future.succeededFuture(new TextResourceContent()
        .setUri("resource://user/" + params.get("id"))
        .setName("user-" + params.get("id"))
        .setText("User data"))
    );

    feature.addDynamicResource("resource://file/{path}/content", params ->
      Future.succeededFuture(new TextResourceContent()
        .setUri("resource://file/" + params.get("path") + "/content")
        .setName("file-content")
        .setText("File content"))
    );

    sendRequest(HttpMethod.POST, new ListResourceTemplatesRequest())
      .compose(HttpClientResponse::body)
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNull(response.getError(), "Should succeed");
        JsonObject result = (JsonObject) response.getResult();

        JsonArray templates = result.getJsonArray("resourceTemplates");
        context.assertNotNull(templates, "Should have resourceTemplates array");
        context.assertEquals(2, templates.size(), "Should have 2 templates");

        // Verify template structure
        JsonObject template1 = templates.getJsonObject(0);
        context.assertEquals("resource://user/{id}", template1.getString("uriTemplate"));

        JsonObject template2 = templates.getJsonObject(1);
        context.assertEquals("resource://file/{path}/content", template2.getString("uriTemplate"));

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testReadDynamicResourceWithSingleVariable(TestContext context) {
    Async async = context.async();

    // Add dynamic resource with single variable
    feature.addDynamicResource("resource://user/{id}", params ->
      Future.succeededFuture(new TextResourceContent()
        .setUri("resource://user/" + params.get("id"))
        .setName("user-" + params.get("id"))
        .setTitle("User " + params.get("id"))
        .setDescription("User details")
        .setText("User data for " + params.get("id")))
    );

    sendRequest(HttpMethod.POST, new ReadResourceRequest(new JsonObject().put("uri", "resource://user/123")))
      .compose(HttpClientResponse::body)
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNull(response.getError(), "Should succeed");
        JsonObject result = (JsonObject) response.getResult();

        JsonArray contents = result.getJsonArray("contents");
        context.assertNotNull(contents, "Should have contents array");
        context.assertEquals(1, contents.size(), "Should have 1 content item");

        JsonObject content = contents.getJsonObject(0);
        context.assertEquals("resource://user/123", content.getString("uri"));
        context.assertEquals("user-123", content.getString("name"));
        context.assertEquals("User 123", content.getString("title"));

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testReadDynamicResourceWithMultipleVariables(TestContext context) {
    Async async = context.async();

    // Add dynamic resource with multiple variables
    feature.addDynamicResource("resource://project/{projectId}/file/{fileId}", params ->
      Future.succeededFuture(new TextResourceContent()
        .setUri("resource://project/" + params.get("projectId") + "/file/" + params.get("fileId"))
        .setName("project-file")
        .setText("File content for project " + params.get("projectId") + " file " + params.get("fileId")))
    );

    sendRequest(HttpMethod.POST, new ReadResourceRequest(new JsonObject().put("uri", "resource://project/proj-1/file/file-2")))
      .compose(HttpClientResponse::body)
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNull(response.getError(), "Should succeed");
        JsonObject result = (JsonObject) response.getResult();

        JsonArray contents = result.getJsonArray("contents");
        context.assertNotNull(contents, "Should have contents array");
        context.assertEquals(1, contents.size(), "Should have 1 content item");

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testDynamicResourceNotFoundWrongPattern(TestContext context) {
    Async async = context.async();

    // Add dynamic resource
    feature.addDynamicResource("resource://user/{id}", params ->
      Future.succeededFuture(new TextResourceContent()
        .setUri("resource://user/" + params.get("id"))
        .setName("user-" + params.get("id"))
        .setText("User data"))
    );

    // Try to read with wrong pattern (too many segments)
    sendRequest(HttpMethod.POST, new ReadResourceRequest(new JsonObject().put("uri", "resource://user/123/extra")))
      .compose(HttpClientResponse::body)
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
  public void testDynamicResourcesNotInListResources(TestContext context) {
    Async async = context.async();

    // Add static and dynamic resources
    feature.addStaticResource("static-resource", () ->
      Future.succeededFuture(new TextResourceContent()
        .setUri("resource://static-resource")
        .setName("static-resource")
        .setText("Static content"))
    );

    feature.addDynamicResource("resource://user/{id}", params ->
      Future.succeededFuture(new TextResourceContent()
        .setUri("resource://user/" + params.get("id"))
        .setName("user-" + params.get("id"))
        .setText("User data"))
    );

    sendRequest(HttpMethod.POST, new ListResourcesRequest())
      .compose(HttpClientResponse::body)
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNull(response.getError(), "Should succeed");
        JsonObject result = (JsonObject) response.getResult();

        JsonArray resources = result.getJsonArray("resources");
        context.assertNotNull(resources, "Should have resources array");
        // Only static resources should be listed, not dynamic ones
        context.assertEquals(1, resources.size(), "Should have only 1 static resource");

        JsonObject resource = resources.getJsonObject(0);
        context.assertEquals("resource://static-resource", resource.getString("uri"));

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testDynamicResourceHandlerFailure(TestContext context) {
    Async async = context.async();

    // Add dynamic resource that fails
    feature.addDynamicResource("resource://failing/{id}", params ->
      Future.failedFuture("Dynamic resource generation failed")
    );

    sendRequest(HttpMethod.POST, new ReadResourceRequest(new JsonObject().put("uri", "resource://failing/123")))
      .compose(HttpClientResponse::body)
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNotNull(response.getError(), "Should have error");
        context.assertEquals(-32603, response.getError().getCode(), "Should be internal error");
        context.assertTrue(response.getError().getMessage().contains("failed"));

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testDynamicResourceVariableAtStart(TestContext context) {
    Async async = context.async();

    // Add dynamic resource with variable at start
    feature.addDynamicResource("{type}://resource/data", params ->
      Future.succeededFuture(new TextResourceContent()
        .setUri(params.get("type") + "://resource/data")
        .setName("typed-resource")
        .setText("Resource with variable at start: " + params.get("type")))
    );

    sendRequest(HttpMethod.POST, new ReadResourceRequest(new JsonObject().put("uri", "file://resource/data")))
      .compose(HttpClientResponse::body)
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNull(response.getError(), "Should succeed");
        JsonObject result = (JsonObject) response.getResult();

        JsonArray contents = result.getJsonArray("contents");
        context.assertEquals(1, contents.size(), "Should have 1 content item");

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testDynamicResourceVariableAtEnd(TestContext context) {
    Async async = context.async();

    // Add dynamic resource with variable at end
    feature.addDynamicResource("resource://api/user/{userId}", params ->
      Future.succeededFuture(new TextResourceContent()
        .setUri("resource://api/user/" + params.get("userId"))
        .setName("api-user")
        .setText("API user data for user " + params.get("userId")))
    );

    sendRequest(HttpMethod.POST, new ReadResourceRequest(new JsonObject().put("uri", "resource://api/user/456")))
      .compose(HttpClientResponse::body)
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNull(response.getError(), "Should succeed");
        JsonObject result = (JsonObject) response.getResult();

        JsonArray contents = result.getJsonArray("contents");
        context.assertEquals(1, contents.size(), "Should have 1 content item");

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testMultipleDynamicResourceTemplates(TestContext context) {
    Async async = context.async();

    // Add multiple dynamic resources
    feature.addDynamicResource("resource://user/{id}", params ->
      Future.succeededFuture(new TextResourceContent()
        .setUri("resource://user/" + params.get("id"))
        .setName("user")
        .setText("User " + params.get("id")))
    );

    feature.addDynamicResource("resource://post/{postId}", params ->
      Future.succeededFuture(new TextResourceContent()
        .setUri("resource://post/" + params.get("postId"))
        .setName("post")
        .setText("Post " + params.get("postId")))
    );

    feature.addDynamicResource("resource://comment/{commentId}", params ->
      Future.succeededFuture(new TextResourceContent()
        .setUri("resource://comment/" + params.get("commentId"))
        .setName("comment")
        .setText("Comment " + params.get("commentId")))
    );

    sendRequest(HttpMethod.POST, new ListResourceTemplatesRequest())
      .compose(HttpClientResponse::body)
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNull(response.getError(), "Should succeed");
        JsonObject result = (JsonObject) response.getResult();

        JsonArray templates = result.getJsonArray("resourceTemplates");
        context.assertEquals(3, templates.size(), "Should have 3 templates");

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }
}
