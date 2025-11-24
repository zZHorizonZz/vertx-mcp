package io.vertx.tests.server;

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
import io.vertx.mcp.common.resources.ResourceTemplate;
import io.vertx.mcp.common.resources.TextResourceContent;
import io.vertx.mcp.common.result.ListResourceTemplatesResult;
import io.vertx.mcp.common.result.ListResourcesResult;
import io.vertx.mcp.common.result.ReadResourceResult;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.server.feature.ResourceServerFeature;
import org.junit.Test;

import java.util.List;

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

        ListResourcesResult result = new ListResourcesResult((JsonObject) response.getResult());

        JsonArray resources = result.getResources();
        context.assertNotNull(resources, "Should have resources array");
        context.assertEquals(0, resources.size(), "Should be empty");

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testListResourcesWithStaticResources(TestContext context) {
    feature.addStaticResource("resource://test-resource-1", "test-resource-1", () ->
      Future.succeededFuture(new TextResourceContent()
        .setUri("resource://test-resource-1")
        .setName("test-resource-1")
        .setText("Test content 1"))
    );

    feature.addStaticResource("resource://test-resource-2", "test-resource-2", () ->
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

        ListResourcesResult result = new ListResourcesResult((JsonObject) response.getResult());

        JsonArray resources = result.getResources();
        context.assertNotNull(resources, "Should have resources array");
        context.assertEquals(2, resources.size(), "Should have 2 resources");

        // Check both resources exist (order not guaranteed)
        boolean found1 = false, found2 = false;
        for (int i = 0; i < resources.size(); i++) {
          JsonObject resource = resources.getJsonObject(i);
          String uri = resource.getString("uri");
          if ("resource://test-resource-1".equals(uri)) {
            context.assertEquals("test-resource-1", resource.getString("name"));
            found1 = true;
          } else if ("resource://test-resource-2".equals(uri)) {
            context.assertEquals("test-resource-2", resource.getString("name"));
            found2 = true;
          }
        }
        context.assertTrue(found1, "Should have test-resource-1");
        context.assertTrue(found2, "Should have test-resource-2");

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testReadStaticResource(TestContext context) {
    Async async = context.async();
    String testContent = "This is test content";

    feature.addStaticResource("resource://test-resource", "test-resource", () ->
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

        ReadResourceResult result = new ReadResourceResult((JsonObject) response.getResult());

        JsonArray contents = result.getContents();
        context.assertNotNull(contents, "Should have contents array");
        context.assertEquals(1, contents.size(), "Should have 1 content item");

        TextResourceContent content = new TextResourceContent(contents.getJsonObject(0));
        context.assertEquals("resource://test-resource", content.getUri());
        context.assertEquals("test-resource", content.getName());
        context.assertEquals("Test Resource", content.getTitle());
        context.assertEquals("A test resource", content.getDescription());
        context.assertEquals("text/plain", content.getMimeType());
        context.assertEquals(testContent, content.getText());

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

        ListResourceTemplatesResult result = new ListResourceTemplatesResult((JsonObject) response.getResult());
        context.assertNotNull(result, "Should have result");

        List<ResourceTemplate> templates = result.getResourceTemplates();
        context.assertNotNull(templates, "Should have resourceTemplates array");

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testResourceHandlerFailure(TestContext context) {
    Async async = context.async();

    feature.addStaticResource("resource://failing-resource", "failing-resource", () ->
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

    for (int i = 0; i < 5; i++) {
      final int index = i;
      feature.addStaticResource("resource://resource-" + index, "resource-" + i, () ->
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

        ListResourcesResult result = new ListResourcesResult((JsonObject) response.getResult());
        context.assertEquals(5, result.getResources().size(), "Should have 5 resources");

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testListResourceTemplatesWithDynamicResources(TestContext context) {
    Async async = context.async();

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

        ListResourceTemplatesResult result = new ListResourceTemplatesResult((JsonObject) response.getResult());

        List<ResourceTemplate> templates = result.getResourceTemplates();
        context.assertNotNull(templates, "Should have resourceTemplates array");
        context.assertEquals(2, templates.size(), "Should have 2 templates");

        // Check both templates exist (order not guaranteed)
        boolean found1 = false, found2 = false;
        for (ResourceTemplate template : templates) {
          String uri = template.getUriTemplate();
          if ("resource://user/{id}".equals(uri)) {
            found1 = true;
          } else if ("resource://file/{path}/content".equals(uri)) {
            found2 = true;
          }
        }
        context.assertTrue(found1, "Should have user template");
        context.assertTrue(found2, "Should have file template");

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testReadDynamicResourceWithSingleVariable(TestContext context) {
    Async async = context.async();

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

        ReadResourceResult result = new ReadResourceResult((JsonObject) response.getResult());

        JsonArray contents = result.getContents();
        context.assertNotNull(contents, "Should have contents array");
        context.assertEquals(1, contents.size(), "Should have 1 content item");

        TextResourceContent content = new TextResourceContent(contents.getJsonObject(0));
        context.assertEquals("resource://user/123", content.getUri());
        context.assertEquals("user-123", content.getName());
        context.assertEquals("User 123", content.getTitle());

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testReadDynamicResourceWithMultipleVariables(TestContext context) {
    Async async = context.async();

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

        ReadResourceResult result = new ReadResourceResult((JsonObject) response.getResult());

        JsonArray contents = result.getContents();
        context.assertNotNull(contents, "Should have contents array");
        context.assertEquals(1, contents.size(), "Should have 1 content item");

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testDynamicResourceNotFoundWrongPattern(TestContext context) {
    Async async = context.async();

    feature.addDynamicResource("resource://user/{id}", params ->
      Future.succeededFuture(new TextResourceContent()
        .setUri("resource://user/" + params.get("id"))
        .setName("user-" + params.get("id"))
        .setText("User data"))
    );

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

        ListResourcesResult result = new ListResourcesResult((JsonObject) response.getResult());

        JsonArray resources = result.getResources();
        context.assertNotNull(resources, "Should have resources array");
        context.assertEquals(1, resources.size(), "Should have only 1 static resource");

        JsonObject resource = resources.getJsonObject(0);
        context.assertEquals("static-resource", resource.getString("uri"));

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testDynamicResourceHandlerFailure(TestContext context) {
    Async async = context.async();

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

        ReadResourceResult result = new ReadResourceResult((JsonObject) response.getResult());

        JsonArray contents = result.getContents();
        context.assertEquals(1, contents.size(), "Should have 1 content item");

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testDynamicResourceVariableAtEnd(TestContext context) {
    Async async = context.async();

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

        ReadResourceResult result = new ReadResourceResult((JsonObject) response.getResult());

        JsonArray contents = result.getContents();
        context.assertEquals(1, contents.size(), "Should have 1 content item");

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testMultipleDynamicResourceTemplates(TestContext context) {
    Async async = context.async();

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

        ListResourceTemplatesResult result = new ListResourceTemplatesResult((JsonObject) response.getResult());

        List<ResourceTemplate> templates = result.getResourceTemplates();
        context.assertEquals(3, templates.size(), "Should have 3 templates");

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }
}
