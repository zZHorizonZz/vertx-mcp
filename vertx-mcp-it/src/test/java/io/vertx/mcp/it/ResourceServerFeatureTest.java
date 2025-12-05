package io.vertx.mcp.it;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.mcp.client.ClientRequestException;
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
import io.vertx.mcp.server.ModelContextProtocolServer;
import io.vertx.mcp.server.feature.ResourceServerFeature;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class ResourceServerFeatureTest extends HttpTransportTestBase {

  private ResourceServerFeature resourceFeature;

  @Before
  public void setUpFeatures(TestContext context) {
    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx);
    super.startServer(context, server);

    resourceFeature = new ResourceServerFeature();
    server.addServerFeature(resourceFeature);
  }

  @Test
  public void testListResourcesEmpty(TestContext context) throws Throwable {
    ListResourcesResult result = (ListResourcesResult) getClient().sendRequest(new ListResourcesRequest())
      .expecting(r -> r instanceof ListResourcesResult)
      .await(10, TimeUnit.SECONDS);

    JsonArray resources = result.getResources();
    context.assertNotNull(resources, "Should have resources array");
    context.assertEquals(0, resources.size(), "Should be empty");
  }

  @Test
  public void testListResourcesWithStaticResources(TestContext context) throws Throwable {
    resourceFeature.addStaticResource("resource://test-resource-1", "test-resource-1", () ->
      Future.succeededFuture(new TextResourceContent()
        .setUri("resource://test-resource-1")
        .setName("test-resource-1")
        .setText("Test content 1"))
    );

    resourceFeature.addStaticResource("resource://test-resource-2", "test-resource-2", () ->
      Future.succeededFuture(new TextResourceContent()
        .setUri("resource://test-resource-2")
        .setName("test-resource-2")
        .setText("Test content 2"))
    );

    ListResourcesResult result = (ListResourcesResult) getClient().sendRequest(new ListResourcesRequest())
      .expecting(r -> r instanceof ListResourcesResult)
      .await(10, TimeUnit.SECONDS);

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

    resourceFeature.addStaticResource("resource://test-resource", "test-resource", () ->
      Future.succeededFuture(new TextResourceContent()
        .setUri("resource://test-resource")
        .setName("test-resource")
        .setTitle("Test Resource")
        .setDescription("A test resource")
        .setMimeType("text/plain")
        .setText(testContent))
    );

    ReadResourceResult result = (ReadResourceResult) getClient().sendRequest(new ReadResourceRequest(new JsonObject().put("uri", "resource://test-resource")))
      .expecting(r -> r instanceof ReadResourceResult)
      .await(10, TimeUnit.SECONDS);

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
    try {
      getClient().sendRequest(new ReadResourceRequest(new JsonObject().put("uri", "resource://nonexistent")))
        .await(10, TimeUnit.SECONDS);
      context.fail("Should have thrown ClientRequestException");
    } catch (ClientRequestException e) {
      context.assertEquals(JsonError.INVALID_PARAMS, e.getCode(), "Should be invalid params");
      context.assertTrue(e.getMessage().contains("not found"));
    }
  }

  @Test
  public void testReadResourceMissingUri(TestContext context) throws Throwable {
    try {
      createSession()
        .compose(session -> session.sendRequest(JsonRequest.createRequest("resources/read", new JsonObject(), 1)))
        .await(10, TimeUnit.SECONDS);
      context.fail("Should have thrown ClientRequestException");
    } catch (ClientRequestException e) {
      context.assertEquals(JsonError.INVALID_PARAMS, e.getCode(), "Should be invalid params");
      context.assertTrue(e.getMessage().contains("Missing 'uri'"));
    }
  }

  @Test
  public void testListResourceTemplates(TestContext context) throws Throwable {
    ListResourceTemplatesResult result = (ListResourceTemplatesResult) getClient().sendRequest(new ListResourceTemplatesRequest())
      .expecting(r -> r instanceof ListResourceTemplatesResult)
      .await(10, TimeUnit.SECONDS);

    context.assertNotNull(result, "Should have result");

    List<ResourceTemplate> templates = result.getResourceTemplates();
    context.assertNotNull(templates, "Should have resourceTemplates array");
  }

  @Test
  public void testResourceHandlerFailure(TestContext context) throws Throwable {
    resourceFeature.addStaticResource("resource://failing-resource", "failing-resource", () ->
      Future.failedFuture("Resource generation failed")
    );

    try {
      getClient().sendRequest(new ReadResourceRequest(new JsonObject().put("uri", "resource://failing-resource")))
        .await(10, TimeUnit.SECONDS);
      context.fail("Should have thrown ClientRequestException");
    } catch (ClientRequestException e) {
      context.assertEquals(JsonError.INTERNAL_ERROR, e.getCode(), "Should be internal error");
    }
  }

  @Test
  public void testUnsupportedResourceMethod(TestContext context) throws Throwable {
    try {
      createSession()
        .compose(session -> session.sendRequest(JsonRequest.createRequest("resources/unsupported", new JsonObject(), 1)))
        .await(10, TimeUnit.SECONDS);
      context.fail("Should have thrown ClientRequestException");
    } catch (ClientRequestException e) {
      context.assertEquals(JsonError.METHOD_NOT_FOUND, e.getCode(), "Should be method not found");
    }
  }

  @Test
  public void testMultipleStaticResources(TestContext context) throws Throwable {
    for (int i = 0; i < 5; i++) {
      final int index = i;
      resourceFeature.addStaticResource("resource://resource-" + index, "resource-" + i, () ->
        Future.succeededFuture(new TextResourceContent()
          .setUri("resource://resource-" + index)
          .setName("resource-" + index)
          .setText("Content " + index))
      );
    }

    ListResourcesResult result = (ListResourcesResult) getClient().sendRequest(new ListResourcesRequest())
      .expecting(r -> r instanceof ListResourcesResult)
      .await(10, TimeUnit.SECONDS);

    context.assertEquals(5, result.getResources().size(), "Should have 5 resources");
  }

  @Test
  public void testListResourceTemplatesWithDynamicResources(TestContext context) throws Throwable {
    resourceFeature.addDynamicResource("resource://user/{id}", params ->
      Future.succeededFuture(new TextResourceContent()
        .setUri("resource://user/" + params.get("id"))
        .setName("user-" + params.get("id"))
        .setText("User data"))
    );

    resourceFeature.addDynamicResource("resource://file/{path}/content", params ->
      Future.succeededFuture(new TextResourceContent()
        .setUri("resource://file/" + params.get("path") + "/content")
        .setName("file-content")
        .setText("File content"))
    );

    ListResourceTemplatesResult result = (ListResourceTemplatesResult) getClient().sendRequest(new ListResourceTemplatesRequest())
      .expecting(r -> r instanceof ListResourceTemplatesResult)
      .await(10, TimeUnit.SECONDS);

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
    resourceFeature.addDynamicResource("resource://user/{id}", params ->
      Future.succeededFuture(new TextResourceContent()
        .setUri("resource://user/" + params.get("id"))
        .setName("user-" + params.get("id"))
        .setTitle("User " + params.get("id"))
        .setDescription("User details")
        .setText("User data for " + params.get("id")))
    );

    ReadResourceResult result = (ReadResourceResult) getClient().sendRequest(new ReadResourceRequest(new JsonObject().put("uri", "resource://user/123")))
      .expecting(r -> r instanceof ReadResourceResult)
      .await(10, TimeUnit.SECONDS);

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
    resourceFeature.addDynamicResource("resource://project/{projectId}/file/{fileId}", params ->
      Future.succeededFuture(new TextResourceContent()
        .setUri("resource://project/" + params.get("projectId") + "/file/" + params.get("fileId"))
        .setName("project-file")
        .setText("File content for project " + params.get("projectId") + " file " + params.get("fileId")))
    );

    ReadResourceResult result = (ReadResourceResult) getClient().sendRequest(new ReadResourceRequest(new JsonObject().put("uri", "resource://project/proj-1/file/file-2")))
      .expecting(r -> r instanceof ReadResourceResult)
      .await(10, TimeUnit.SECONDS);

    JsonArray contents = result.getContents();
    context.assertNotNull(contents, "Should have contents array");
    context.assertEquals(1, contents.size(), "Should have 1 content item");
  }

  @Test
  public void testDynamicResourceNotFoundWrongPattern(TestContext context) throws Throwable {
    resourceFeature.addDynamicResource("resource://user/{id}", params ->
      Future.succeededFuture(new TextResourceContent()
        .setUri("resource://user/" + params.get("id"))
        .setName("user-" + params.get("id"))
        .setText("User data"))
    );

    try {
      getClient().sendRequest(new ReadResourceRequest(new JsonObject().put("uri", "resource://user/123/extra")))
        .await(10, TimeUnit.SECONDS);
      context.fail("Should have thrown ClientRequestException");
    } catch (ClientRequestException e) {
      context.assertEquals(JsonError.INVALID_PARAMS, e.getCode(), "Should be invalid params");
      context.assertTrue(e.getMessage().contains("not found"));
    }
  }

  @Test
  public void testDynamicResourcesNotInListResources(TestContext context) throws Throwable {
    resourceFeature.addStaticResource("static-resource", () ->
      Future.succeededFuture(new TextResourceContent()
        .setUri("resource://static-resource")
        .setName("static-resource")
        .setText("Static content"))
    );

    resourceFeature.addDynamicResource("resource://user/{id}", params ->
      Future.succeededFuture(new TextResourceContent()
        .setUri("resource://user/" + params.get("id"))
        .setName("user-" + params.get("id"))
        .setText("User data"))
    );

    ListResourcesResult result = (ListResourcesResult) getClient().sendRequest(new ListResourcesRequest())
      .expecting(r -> r instanceof ListResourcesResult)
      .await(10, TimeUnit.SECONDS);

    JsonArray resources = result.getResources();
    context.assertNotNull(resources, "Should have resources array");
    context.assertEquals(1, resources.size(), "Should have only 1 static resource");

    JsonObject resource = resources.getJsonObject(0);
    context.assertEquals("static-resource", resource.getString("uri"));
  }

  @Test
  public void testDynamicResourceHandlerFailure(TestContext context) throws Throwable {
    resourceFeature.addDynamicResource("resource://failing/{id}", params ->
      Future.failedFuture("Dynamic resource generation failed")
    );

    try {
      getClient().sendRequest(new ReadResourceRequest(new JsonObject().put("uri", "resource://failing/123")))
        .await(10, TimeUnit.SECONDS);
      context.fail("Should have thrown ClientRequestException");
    } catch (ClientRequestException e) {
      context.assertEquals(JsonError.INTERNAL_ERROR, e.getCode(), "Should be internal error");
      context.assertTrue(e.getMessage().contains("failed"));
    }
  }

  @Test
  public void testDynamicResourceVariableAtStart(TestContext context) throws Throwable {
    resourceFeature.addDynamicResource("{type}://resource/data", params ->
      Future.succeededFuture(new TextResourceContent()
        .setUri(params.get("type") + "://resource/data")
        .setName("typed-resource")
        .setText("Resource with variable at start: " + params.get("type")))
    );

    ReadResourceResult result = (ReadResourceResult) getClient().sendRequest(new ReadResourceRequest(new JsonObject().put("uri", "file://resource/data")))
      .expecting(r -> r instanceof ReadResourceResult)
      .await(10, TimeUnit.SECONDS);

    JsonArray contents = result.getContents();
    context.assertEquals(1, contents.size(), "Should have 1 content item");
  }

  @Test
  public void testDynamicResourceVariableAtEnd(TestContext context) throws Throwable {
    resourceFeature.addDynamicResource("resource://api/user/{userId}", params ->
      Future.succeededFuture(new TextResourceContent()
        .setUri("resource://api/user/" + params.get("userId"))
        .setName("api-user")
        .setText("API user data for user " + params.get("userId")))
    );

    ReadResourceResult result = (ReadResourceResult) getClient().sendRequest(new ReadResourceRequest(new JsonObject().put("uri", "resource://api/user/456")))
      .expecting(r -> r instanceof ReadResourceResult)
      .await(10, TimeUnit.SECONDS);

    JsonArray contents = result.getContents();
    context.assertEquals(1, contents.size(), "Should have 1 content item");
  }

  @Test
  public void testMultipleDynamicResourceTemplates(TestContext context) throws Throwable {
    resourceFeature.addDynamicResource("resource://user/{id}", params ->
      Future.succeededFuture(new TextResourceContent()
        .setUri("resource://user/" + params.get("id"))
        .setName("user")
        .setText("User " + params.get("id")))
    );

    resourceFeature.addDynamicResource("resource://post/{postId}", params ->
      Future.succeededFuture(new TextResourceContent()
        .setUri("resource://post/" + params.get("postId"))
        .setName("post")
        .setText("Post " + params.get("postId")))
    );

    resourceFeature.addDynamicResource("resource://comment/{commentId}", params ->
      Future.succeededFuture(new TextResourceContent()
        .setUri("resource://comment/" + params.get("commentId"))
        .setName("comment")
        .setText("Comment " + params.get("commentId")))
    );

    ListResourceTemplatesResult result = (ListResourceTemplatesResult) getClient().sendRequest(new ListResourceTemplatesRequest())
      .expecting(r -> r instanceof ListResourceTemplatesResult)
      .await(10, TimeUnit.SECONDS);

    List<ResourceTemplate> templates = result.getResourceTemplates();
    context.assertEquals(3, templates.size(), "Should have 3 templates");
  }
}
