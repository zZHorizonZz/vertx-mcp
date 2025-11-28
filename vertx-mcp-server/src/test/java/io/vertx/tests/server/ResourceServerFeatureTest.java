package io.vertx.tests.server;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.mcp.common.request.ListResourceTemplatesRequest;
import io.vertx.mcp.common.request.ListResourcesRequest;
import io.vertx.mcp.common.request.ReadResourceRequest;
import io.vertx.mcp.common.resources.ResourceTemplate;
import io.vertx.mcp.common.resources.TextResourceContent;
import io.vertx.mcp.common.result.ListResourceTemplatesResult;
import io.vertx.mcp.common.result.ListResourcesResult;
import io.vertx.mcp.common.result.ReadResourceResult;
import io.vertx.mcp.common.rpc.JsonError;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.server.feature.ResourceServerFeature;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class ResourceServerFeatureTest extends ServerFeatureTestBase<ResourceServerFeature> {

  @Override
  protected ResourceServerFeature createFeature() {
    return new ResourceServerFeature();
  }

  @Test
  public void testListResourcesEmpty(TestContext context) throws Throwable {
    JsonResponse response = sendRequest(HttpMethod.POST, new ListResourcesRequest())
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .expecting(JsonResponse::isSuccess)
      .await(10, TimeUnit.SECONDS);

    ListResourcesResult result = new ListResourcesResult((JsonObject) response.getResult());

    JsonArray resources = result.getResources();
    context.assertNotNull(resources, "Should have resources array");
    context.assertEquals(0, resources.size(), "Should be empty");
  }

  @Test
  public void testListResourcesWithStaticResources(TestContext context) throws Throwable {
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

    JsonResponse response = sendRequest(HttpMethod.POST, new ListResourcesRequest())
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .expecting(JsonResponse::isSuccess)
      .await(10, TimeUnit.SECONDS);

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
  }

  @Test
  public void testReadStaticResource(TestContext context) throws Throwable {
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

    JsonResponse response = sendRequest(HttpMethod.POST, new ReadResourceRequest(new JsonObject().put("uri", "resource://test-resource")))
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .expecting(JsonResponse::isSuccess)
      .await(10, TimeUnit.SECONDS);

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
  }

  @Test
  public void testReadResourceNotFound(TestContext context) throws Throwable {
    JsonResponse response = sendRequest(HttpMethod.POST, new ReadResourceRequest(new JsonObject().put("uri", "resource://nonexistent")))
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .await(10, TimeUnit.SECONDS);

    context.assertNotNull(response.getError(), "Should have error");
    context.assertEquals(JsonError.INVALID_PARAMS, response.getError().getCode(), "Should be invalid params");
    context.assertTrue(response.getError().getMessage().contains("not found"));
  }

  @Test
  public void testReadResourceMissingUri(TestContext context) throws Throwable {
    JsonResponse response = sendRequest(HttpMethod.POST, JsonRequest.createRequest("resources/read", new JsonObject(), 1))
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .await(10, TimeUnit.SECONDS);

    context.assertNotNull(response.getError(), "Should have error");
    context.assertEquals(JsonError.INVALID_PARAMS, response.getError().getCode(), "Should be invalid params");
    context.assertTrue(response.getError().getMessage().contains("Missing 'uri'"));
  }

  @Test
  public void testListResourceTemplates(TestContext context) throws Throwable {
    JsonResponse response = sendRequest(HttpMethod.POST, new ListResourceTemplatesRequest())
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .expecting(JsonResponse::isSuccess)
      .await(10, TimeUnit.SECONDS);

    ListResourceTemplatesResult result = new ListResourceTemplatesResult((JsonObject) response.getResult());
    context.assertNotNull(result, "Should have result");

    List<ResourceTemplate> templates = result.getResourceTemplates();
    context.assertNotNull(templates, "Should have resourceTemplates array");
  }

  @Test
  public void testResourceHandlerFailure(TestContext context) throws Throwable {
    feature.addStaticResource("resource://failing-resource", "failing-resource", () ->
      Future.failedFuture("Resource generation failed")
    );

    JsonResponse response = sendRequest(HttpMethod.POST, new ReadResourceRequest(new JsonObject().put("uri", "resource://failing-resource")))
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .await(10, TimeUnit.SECONDS);

    context.assertNotNull(response.getError(), "Should have error");
    context.assertEquals(JsonError.INTERNAL_ERROR, response.getError().getCode(), "Should be internal error");
  }

  @Test
  public void testUnsupportedResourceMethod(TestContext context) throws Throwable {
    JsonResponse response = sendRequest(HttpMethod.POST, JsonRequest.createRequest("resources/unsupported", new JsonObject(), 1))
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .await(10, TimeUnit.SECONDS);

    context.assertNotNull(response.getError(), "Should have error");
    context.assertEquals(JsonError.METHOD_NOT_FOUND, response.getError().getCode(), "Should be method not found");
  }

  @Test
  public void testMultipleStaticResources(TestContext context) throws Throwable {
    for (int i = 0; i < 5; i++) {
      final int index = i;
      feature.addStaticResource("resource://resource-" + index, "resource-" + i, () ->
        Future.succeededFuture(new TextResourceContent()
          .setUri("resource://resource-" + index)
          .setName("resource-" + index)
          .setText("Content " + index))
      );
    }

    JsonResponse response = sendRequest(HttpMethod.POST, new ListResourcesRequest())
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .expecting(JsonResponse::isSuccess)
      .await(10, TimeUnit.SECONDS);

    ListResourcesResult result = new ListResourcesResult((JsonObject) response.getResult());
    context.assertEquals(5, result.getResources().size(), "Should have 5 resources");
  }

  @Test
  public void testListResourceTemplatesWithDynamicResources(TestContext context) throws Throwable {
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

    JsonResponse response = sendRequest(HttpMethod.POST, new ListResourceTemplatesRequest())
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .expecting(JsonResponse::isSuccess)
      .await(10, TimeUnit.SECONDS);

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
  }

  @Test
  public void testReadDynamicResourceWithSingleVariable(TestContext context) throws Throwable {
    feature.addDynamicResource("resource://user/{id}", params ->
      Future.succeededFuture(new TextResourceContent()
        .setUri("resource://user/" + params.get("id"))
        .setName("user-" + params.get("id"))
        .setTitle("User " + params.get("id"))
        .setDescription("User details")
        .setText("User data for " + params.get("id")))
    );

    JsonResponse response = sendRequest(HttpMethod.POST, new ReadResourceRequest(new JsonObject().put("uri", "resource://user/123")))
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .expecting(JsonResponse::isSuccess)
      .await(10, TimeUnit.SECONDS);

    ReadResourceResult result = new ReadResourceResult((JsonObject) response.getResult());

    JsonArray contents = result.getContents();
    context.assertNotNull(contents, "Should have contents array");
    context.assertEquals(1, contents.size(), "Should have 1 content item");

    TextResourceContent content = new TextResourceContent(contents.getJsonObject(0));
    context.assertEquals("resource://user/123", content.getUri());
    context.assertEquals("user-123", content.getName());
    context.assertEquals("User 123", content.getTitle());
  }

  @Test
  public void testReadDynamicResourceWithMultipleVariables(TestContext context) throws Throwable {
    feature.addDynamicResource("resource://project/{projectId}/file/{fileId}", params ->
      Future.succeededFuture(new TextResourceContent()
        .setUri("resource://project/" + params.get("projectId") + "/file/" + params.get("fileId"))
        .setName("project-file")
        .setText("File content for project " + params.get("projectId") + " file " + params.get("fileId")))
    );

    JsonResponse response = sendRequest(HttpMethod.POST, new ReadResourceRequest(new JsonObject().put("uri", "resource://project/proj-1/file/file-2")))
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .expecting(JsonResponse::isSuccess)
      .await(10, TimeUnit.SECONDS);

    ReadResourceResult result = new ReadResourceResult((JsonObject) response.getResult());

    JsonArray contents = result.getContents();
    context.assertNotNull(contents, "Should have contents array");
    context.assertEquals(1, contents.size(), "Should have 1 content item");
  }

  @Test
  public void testDynamicResourceNotFoundWrongPattern(TestContext context) throws Throwable {
    feature.addDynamicResource("resource://user/{id}", params ->
      Future.succeededFuture(new TextResourceContent()
        .setUri("resource://user/" + params.get("id"))
        .setName("user-" + params.get("id"))
        .setText("User data"))
    );

    JsonResponse response = sendRequest(HttpMethod.POST, new ReadResourceRequest(new JsonObject().put("uri", "resource://user/123/extra")))
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .await(10, TimeUnit.SECONDS);

    context.assertNotNull(response.getError(), "Should have error");
    context.assertEquals(JsonError.INVALID_PARAMS, response.getError().getCode(), "Should be invalid params");
    context.assertTrue(response.getError().getMessage().contains("not found"));
  }

  @Test
  public void testDynamicResourcesNotInListResources(TestContext context) throws Throwable {
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

    JsonResponse response = sendRequest(HttpMethod.POST, new ListResourcesRequest())
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .expecting(JsonResponse::isSuccess)
      .await(10, TimeUnit.SECONDS);

    ListResourcesResult result = new ListResourcesResult((JsonObject) response.getResult());

    JsonArray resources = result.getResources();
    context.assertNotNull(resources, "Should have resources array");
    context.assertEquals(1, resources.size(), "Should have only 1 static resource");

    JsonObject resource = resources.getJsonObject(0);
    context.assertEquals("static-resource", resource.getString("uri"));
  }

  @Test
  public void testDynamicResourceHandlerFailure(TestContext context) throws Throwable {
    feature.addDynamicResource("resource://failing/{id}", params ->
      Future.failedFuture("Dynamic resource generation failed")
    );

    JsonResponse response = sendRequest(HttpMethod.POST, new ReadResourceRequest(new JsonObject().put("uri", "resource://failing/123")))
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .await(10, TimeUnit.SECONDS);

    context.assertNotNull(response.getError(), "Should have error");
    context.assertEquals(JsonError.INTERNAL_ERROR, response.getError().getCode(), "Should be internal error");
    context.assertTrue(response.getError().getMessage().contains("failed"));
  }

  @Test
  public void testDynamicResourceVariableAtStart(TestContext context) throws Throwable {
    feature.addDynamicResource("{type}://resource/data", params ->
      Future.succeededFuture(new TextResourceContent()
        .setUri(params.get("type") + "://resource/data")
        .setName("typed-resource")
        .setText("Resource with variable at start: " + params.get("type")))
    );

    JsonResponse response = sendRequest(HttpMethod.POST, new ReadResourceRequest(new JsonObject().put("uri", "file://resource/data")))
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .expecting(JsonResponse::isSuccess)
      .await(10, TimeUnit.SECONDS);

    ReadResourceResult result = new ReadResourceResult((JsonObject) response.getResult());

    JsonArray contents = result.getContents();
    context.assertEquals(1, contents.size(), "Should have 1 content item");
  }

  @Test
  public void testDynamicResourceVariableAtEnd(TestContext context) throws Throwable {
    feature.addDynamicResource("resource://api/user/{userId}", params ->
      Future.succeededFuture(new TextResourceContent()
        .setUri("resource://api/user/" + params.get("userId"))
        .setName("api-user")
        .setText("API user data for user " + params.get("userId")))
    );

    JsonResponse response = sendRequest(HttpMethod.POST, new ReadResourceRequest(new JsonObject().put("uri", "resource://api/user/456")))
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .expecting(JsonResponse::isSuccess)
      .await(10, TimeUnit.SECONDS);

    ReadResourceResult result = new ReadResourceResult((JsonObject) response.getResult());

    JsonArray contents = result.getContents();
    context.assertEquals(1, contents.size(), "Should have 1 content item");
  }

  @Test
  public void testMultipleDynamicResourceTemplates(TestContext context) throws Throwable {
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

    JsonResponse response = sendRequest(HttpMethod.POST, new ListResourceTemplatesRequest())
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .expecting(JsonResponse::isSuccess)
      .await(10, TimeUnit.SECONDS);

    ListResourceTemplatesResult result = new ListResourceTemplatesResult((JsonObject) response.getResult());

    List<ResourceTemplate> templates = result.getResourceTemplates();
    context.assertEquals(3, templates.size(), "Should have 3 templates");
  }
}
