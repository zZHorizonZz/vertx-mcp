package io.vertx.tests.server;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.json.schema.common.dsl.Schemas;
import io.vertx.mcp.common.completion.Completion;
import io.vertx.mcp.common.content.TextContent;
import io.vertx.mcp.common.prompt.PromptMessage;
import io.vertx.mcp.common.resources.TextResourceContent;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.server.ModelContextProtocolServer;
import io.vertx.mcp.server.PromptHandler;
import io.vertx.mcp.server.feature.CompletionServerFeature;
import io.vertx.mcp.server.feature.PromptServerFeature;
import io.vertx.mcp.server.feature.ResourceServerFeature;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CompletionServerFeatureTest extends HttpTransportTestBase {

  private CompletionServerFeature completionFeature;
  private PromptServerFeature promptFeature;
  private ResourceServerFeature resourceFeature;

  @Before
  public void setUpFeatures(TestContext context) {
    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx);
    super.startServer(context, server);

    completionFeature = new CompletionServerFeature(server);
    promptFeature = new PromptServerFeature();
    resourceFeature = new ResourceServerFeature();

    mcpServer.addServerFeature(completionFeature);
    mcpServer.addServerFeature(promptFeature);
    mcpServer.addServerFeature(resourceFeature);
  }

  @Test
  public void testCompletionForPromptWithHandler(TestContext context) {
    Async async = context.async();

    // Add a prompt with completion support
    promptFeature.addPrompt(PromptHandler.create(
      "code_review",
      "Code Review",
      "Reviews code",
      Schemas.arraySchema().items(
        Schemas.objectSchema()
          .requiredProperty("language", Schemas.stringSchema())
      ),
      args -> {
        List<PromptMessage> messages = new ArrayList<>();
        messages.add(new PromptMessage()
          .setRole("user")
          .setContent(new TextContent("Review code").toJson()));
        return Future.succeededFuture(messages);
      },
      (argument, completionContext) -> {
        if ("language".equals(argument.getName())) {
          String prefix = argument.getValue() != null ? argument.getValue() : "";
          List<String> languages = Arrays.asList("java", "javascript", "python", "go", "rust");
          List<String> filtered = languages.stream()
            .filter(lang -> lang.startsWith(prefix.toLowerCase()))
            .collect(Collectors.toList());
          return Future.succeededFuture(new Completion()
            .setValues(filtered)
            .setTotal(filtered.size())
            .setHasMore(false));
        }
        return Future.succeededFuture(new Completion()
          .setValues(new ArrayList<>())
          .setTotal(0)
          .setHasMore(false));
      }
    ));

    // Send completion request
    JsonObject params = new JsonObject()
      .put("ref", new JsonObject()
        .put("type", "ref/prompt")
        .put("name", "code_review"))
      .put("argument", new JsonObject()
        .put("name", "language")
        .put("value", "ja"));

    sendRequest(HttpMethod.POST, JsonRequest.createRequest("completion/complete", params, 1))
      .compose(HttpClientResponse::body)
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNull(response.getError(), "Should succeed");
        JsonObject result = (JsonObject) response.getResult();
        context.assertNotNull(result, "Should have result");

        JsonObject completion = result.getJsonObject("completion");
        context.assertNotNull(completion, "Should have completion");

        JsonArray values = completion.getJsonArray("values");
        context.assertNotNull(values, "Should have values");
        context.assertEquals(2, values.size(), "Should have 2 matches (java, javascript)");
        context.assertTrue(values.contains("java"));
        context.assertTrue(values.contains("javascript"));

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testCompletionForResourceWithHandler(TestContext context) {
    Async async = context.async();

    // Add a dynamic resource with completion support
    List<String> userIds = Arrays.asList("user-1", "user-2", "user-3", "admin-1");

    resourceFeature.addDynamicResource(
      "resource://user/{id}",
      "user",
      "User Resource",
      "Get user by ID",
      params -> {
        String id = params.get("id");
        return Future.succeededFuture(new TextResourceContent()
          .setUri("resource://user/" + id)
          .setText("User: " + id));
      },
      (argument, completionContext) -> {
        if ("id".equals(argument.getName())) {
          String prefix = argument.getValue() != null ? argument.getValue() : "";
          List<String> filtered = userIds.stream()
            .filter(id -> id.startsWith(prefix))
            .collect(Collectors.toList());
          return Future.succeededFuture(new Completion()
            .setValues(filtered)
            .setTotal(filtered.size())
            .setHasMore(false));
        }
        return Future.succeededFuture(new Completion()
          .setValues(new ArrayList<>())
          .setTotal(0)
          .setHasMore(false));
      }
    );

    // Send completion request
    JsonObject params = new JsonObject()
      .put("ref", new JsonObject()
        .put("type", "ref/resource")
        .put("uri", "resource://user/{id}"))
      .put("argument", new JsonObject()
        .put("name", "id")
        .put("value", "user"));

    sendRequest(HttpMethod.POST, JsonRequest.createRequest("completion/complete", params, 1))
      .compose(HttpClientResponse::body)
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNull(response.getError(), "Should succeed");
        JsonObject result = (JsonObject) response.getResult();

        JsonObject completion = result.getJsonObject("completion");
        context.assertNotNull(completion, "Should have completion");

        JsonArray values = completion.getJsonArray("values");
        context.assertNotNull(values, "Should have values");
        context.assertEquals(3, values.size(), "Should have 3 user matches");

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testCompletionForUnknownPrompt(TestContext context) {
    Async async = context.async();

    // Send completion request for non-existent prompt
    JsonObject params = new JsonObject()
      .put("ref", new JsonObject()
        .put("type", "ref/prompt")
        .put("name", "nonexistent"))
      .put("argument", new JsonObject()
        .put("name", "arg")
        .put("value", ""));

    sendRequest(HttpMethod.POST, JsonRequest.createRequest("completion/complete", params, 1))
      .compose(HttpClientResponse::body)
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNull(response.getError(), "Should succeed with empty completion");
        JsonObject result = (JsonObject) response.getResult();

        JsonObject completion = result.getJsonObject("completion");
        context.assertNotNull(completion, "Should have completion");

        JsonArray values = completion.getJsonArray("values");
        context.assertNotNull(values, "Should have values");
        context.assertEquals(0, values.size(), "Should be empty");

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testCompletionMissingRef(TestContext context) {
    Async async = context.async();

    JsonObject params = new JsonObject()
      .put("argument", new JsonObject()
        .put("name", "arg")
        .put("value", ""));

    sendRequest(HttpMethod.POST, JsonRequest.createRequest("completion/complete", params, 1))
      .compose(HttpClientResponse::body)
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNotNull(response.getError(), "Should have error");
        context.assertEquals(-32602, response.getError().getCode(), "Should be invalid params");
        context.assertTrue(response.getError().getMessage().contains("ref"));

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testCompletionMissingArgument(TestContext context) {
    Async async = context.async();

    JsonObject params = new JsonObject()
      .put("ref", new JsonObject()
        .put("type", "ref/prompt")
        .put("name", "test"));

    sendRequest(HttpMethod.POST, JsonRequest.createRequest("completion/complete", params, 1))
      .compose(HttpClientResponse::body)
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNotNull(response.getError(), "Should have error");
        context.assertEquals(-32602, response.getError().getCode(), "Should be invalid params");
        context.assertTrue(response.getError().getMessage().contains("argument"));

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testCompletionUnknownRefType(TestContext context) {
    Async async = context.async();

    JsonObject params = new JsonObject()
      .put("ref", new JsonObject()
        .put("type", "ref/unknown")
        .put("name", "test"))
      .put("argument", new JsonObject()
        .put("name", "arg")
        .put("value", ""));

    sendRequest(HttpMethod.POST, JsonRequest.createRequest("completion/complete", params, 1))
      .compose(HttpClientResponse::body)
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNotNull(response.getError(), "Should have error");
        context.assertEquals(-32602, response.getError().getCode(), "Should be invalid params");
        context.assertTrue(response.getError().getMessage().contains("Unknown reference type"));

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testCompletionWithContext(TestContext context) {
    Async async = context.async();

    // Add a prompt that uses context for completion
    promptFeature.addPrompt(PromptHandler.create(
      "multi_arg",
      "Multi Arg",
      "Prompt with multiple arguments",
      Schemas.arraySchema().items(
        Schemas.objectSchema()
          .requiredProperty("category", Schemas.stringSchema())
          .requiredProperty("item", Schemas.stringSchema())
      ),
      args -> {
        List<PromptMessage> messages = new ArrayList<>();
        messages.add(new PromptMessage()
          .setRole("user")
          .setContent(new TextContent("Selected: " + args.encode()).toJson()));
        return Future.succeededFuture(messages);
      },
      (argument, completionContext) -> {
        if ("item".equals(argument.getName())) {
          // Get the category from context
          String category = completionContext.getArguments() != null
            ? completionContext.getArguments().get("category")
            : null;

          List<String> items;
          if ("fruit".equals(category)) {
            items = Arrays.asList("apple", "banana", "cherry");
          } else if ("vegetable".equals(category)) {
            items = Arrays.asList("carrot", "broccoli", "spinach");
          } else {
            items = new ArrayList<>();
          }

          String prefix = argument.getValue() != null ? argument.getValue() : "";
          List<String> filtered = items.stream()
            .filter(item -> item.startsWith(prefix.toLowerCase()))
            .collect(Collectors.toList());

          return Future.succeededFuture(new Completion()
            .setValues(filtered)
            .setTotal(filtered.size())
            .setHasMore(false));
        }
        return Future.succeededFuture(new Completion()
          .setValues(new ArrayList<>())
          .setTotal(0)
          .setHasMore(false));
      }
    ));

    // Send completion request with context
    JsonObject params = new JsonObject()
      .put("ref", new JsonObject()
        .put("type", "ref/prompt")
        .put("name", "multi_arg"))
      .put("argument", new JsonObject()
        .put("name", "item")
        .put("value", ""))
      .put("context", new JsonObject()
        .put("arguments", new JsonObject()
          .put("category", "fruit")));

    sendRequest(HttpMethod.POST, JsonRequest.createRequest("completion/complete", params, 1))
      .compose(HttpClientResponse::body)
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNull(response.getError(), "Should succeed");
        JsonObject result = (JsonObject) response.getResult();

        JsonObject completion = result.getJsonObject("completion");
        JsonArray values = completion.getJsonArray("values");
        context.assertEquals(3, values.size(), "Should have 3 fruit items");
        context.assertTrue(values.contains("apple"));
        context.assertTrue(values.contains("banana"));
        context.assertTrue(values.contains("cherry"));

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testCompletionPromptWithoutHandler(TestContext context) {
    Async async = context.async();

    // Add a prompt without completion handler
    promptFeature.addPrompt(
      "simple",
      "Simple Prompt",
      "A prompt without completion",
      null,
      args -> {
        List<PromptMessage> messages = new ArrayList<>();
        messages.add(new PromptMessage()
          .setRole("user")
          .setContent(new TextContent("Hello").toJson()));
        return Future.succeededFuture(messages);
      }
    );

    // Send completion request
    JsonObject params = new JsonObject()
      .put("ref", new JsonObject()
        .put("type", "ref/prompt")
        .put("name", "simple"))
      .put("argument", new JsonObject()
        .put("name", "arg")
        .put("value", ""));

    sendRequest(HttpMethod.POST, JsonRequest.createRequest("completion/complete", params, 1))
      .compose(HttpClientResponse::body)
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNull(response.getError(), "Should succeed with empty completion");
        JsonObject result = (JsonObject) response.getResult();

        JsonObject completion = result.getJsonObject("completion");
        JsonArray values = completion.getJsonArray("values");
        context.assertEquals(0, values.size(), "Should be empty when no handler");

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }
}
