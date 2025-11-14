package io.vertx.mcp.server;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.json.schema.common.dsl.ArraySchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;
import io.vertx.mcp.common.content.TextContent;
import io.vertx.mcp.common.prompt.PromptMessage;
import io.vertx.mcp.common.request.GetPromptRequest;
import io.vertx.mcp.common.request.ListPromptsRequest;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.server.impl.PromptServerFeature;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class PromptServerFeatureTest extends ServerFeatureTestBase<PromptServerFeature> {

  // Common schemas used across tests
  private static final ArraySchemaBuilder CODE_ARGUMENT_SCHEMA = Schemas.arraySchema()
    .items(Schemas.objectSchema().requiredProperty("code", Schemas.stringSchema()));

  @Override
  protected PromptServerFeature createFeature() {
    return new PromptServerFeature();
  }

  @Test
  public void testListPromptsEmpty(TestContext context) {
    Async async = context.async();

    sendRequest(HttpMethod.POST, new ListPromptsRequest())
      .compose(HttpClientResponse::body)
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNull(response.getError(), "Should succeed");
        JsonObject result = (JsonObject) response.getResult();
        context.assertNotNull(result, "Should have result");

        JsonArray prompts = result.getJsonArray("prompts");
        context.assertNotNull(prompts, "Should have prompts array");
        context.assertEquals(0, prompts.size(), "Should be empty");

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testListPromptsWithPrompts(TestContext context) {
    feature.addPrompt(
      "code_review",
      "Code Review",
      "Reviews code and suggests improvements",
      PromptHandler.create(
        CODE_ARGUMENT_SCHEMA,
        args -> {
          List<PromptMessage> messages = new ArrayList<>();
          PromptMessage message = new PromptMessage()
            .setRole("user")
            .setContent(new TextContent("Review this code: " + args.getString("code")).toJson());
          messages.add(message);
          return Future.succeededFuture(messages);
        })
    );

    feature.addPrompt(
      "explain_code",
      "Explain Code",
      "Explains what code does",
      PromptHandler.create(
        CODE_ARGUMENT_SCHEMA,
        args -> {
          List<PromptMessage> messages = new ArrayList<>();
          PromptMessage message = new PromptMessage()
            .setRole("user")
            .setContent(new TextContent("Explain this code: " + args.getString("code")).toJson());
          messages.add(message);
          return Future.succeededFuture(messages);
        })
    );

    Async async = context.async();
    JsonRequest request = new ListPromptsRequest().toRequest(1);

    sendRequest(HttpMethod.POST, request.toJson().toBuffer())
      .compose(resp -> resp.body())
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNull(response.getError(), "Should succeed");
        JsonObject result = (JsonObject) response.getResult();

        JsonArray prompts = result.getJsonArray("prompts");
        context.assertNotNull(prompts, "Should have prompts array");
        context.assertEquals(2, prompts.size(), "Should have 2 prompts");

        // Verify prompt structure
        JsonObject prompt1 = prompts.getJsonObject(0);
        context.assertEquals("code_review", prompt1.getString("name"));
        context.assertEquals("Code Review", prompt1.getString("title"));
        context.assertEquals("Reviews code and suggests improvements", prompt1.getString("description"));

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testGetPrompt(TestContext context) {

    feature.addPrompt(
      "code_review",
      "Code Review",
      "Reviews code and suggests improvements",
      PromptHandler.create(
        Schemas.arraySchema()
          .items(
            Schemas.objectSchema()
              .requiredProperty("code", Schemas.stringSchema())
          ),
        args -> {
          String code = args.getString("code");
          List<PromptMessage> messages = new ArrayList<>();

          PromptMessage message = new PromptMessage()
            .setRole("user")
            .setContent(new TextContent("Please review this code:\n" + code).toJson());
          messages.add(message);

          return Future.succeededFuture(messages);
        })
    );

    Async async = context.async();

    JsonObject params = new JsonObject()
      .put("name", "code_review")
      .put("arguments", new JsonObject()
        .put("code", "def hello():\n    print('world')"));

    JsonRequest request = new GetPromptRequest(params).toRequest(1);

    sendRequest(HttpMethod.POST, request.toJson().toBuffer())
      .compose(resp -> resp.body())
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNull(response.getError(), "Should succeed");
        JsonObject result = (JsonObject) response.getResult();

        context.assertEquals("Reviews code and suggests improvements", result.getString("description"));

        JsonArray messages = result.getJsonArray("messages");
        context.assertNotNull(messages, "Should have messages array");
        context.assertEquals(1, messages.size(), "Should have 1 message");

        JsonObject message = messages.getJsonObject(0);
        context.assertEquals("user", message.getString("role"));

        JsonObject content = message.getJsonObject("content");
        context.assertNotNull(content, "Should have content object");
        context.assertEquals("text", content.getString("type"));
        context.assertTrue(content.getString("text").contains("def hello()"));

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testGetPromptNotFound(TestContext context) {

    Async async = context.async();

    JsonObject params = new JsonObject()
      .put("name", "nonexistent");
    JsonRequest request = new GetPromptRequest(params).toRequest(1);

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
  public void testGetPromptMissingName(TestContext context) {

    Async async = context.async();

    JsonObject params = new JsonObject(); // Missing name parameter
    JsonRequest request = JsonRequest.createRequest("prompts/get", params, 1);

    sendRequest(HttpMethod.POST, request.toJson().toBuffer())
      .compose(resp -> resp.body())
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNotNull(response.getError(), "Should have error");
        context.assertEquals(-32602, response.getError().getCode(), "Should be invalid params");
        context.assertTrue(response.getError().getMessage().contains("Missing 'name'"));

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testPromptHandlerFailure(TestContext context) {

    feature.addPrompt(
      "failing_prompt",
      "Failing Prompt",
      "A prompt that fails",
      PromptHandler.create(null, args -> Future.failedFuture("Prompt generation failed"))
    );

    Async async = context.async();

    JsonObject params = new JsonObject()
      .put("name", "failing_prompt");
    JsonRequest request = new GetPromptRequest(params).toRequest(1);

    sendRequest(HttpMethod.POST, request.toJson().toBuffer())
      .compose(resp -> resp.body())
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
  public void testGetPromptWithMultipleMessages(TestContext context) {

    feature.addPrompt(
      "conversation",
      "Conversation",
      "A multi-turn conversation prompt",
      PromptHandler.create(null, args -> {
        List<PromptMessage> messages = new ArrayList<>();

        PromptMessage userMessage = new PromptMessage()
          .setRole("user")
          .setContent(new TextContent("Hello!").toJson());
        messages.add(userMessage);

        PromptMessage assistantMessage = new PromptMessage()
          .setRole("assistant")
          .setContent(new TextContent("Hi! How can I help you?").toJson());
        messages.add(assistantMessage);

        PromptMessage userMessage2 = new PromptMessage()
          .setRole("user")
          .setContent(new TextContent("Tell me about MCP").toJson());
        messages.add(userMessage2);

        return Future.succeededFuture(messages);
      })
    );

    Async async = context.async();

    JsonObject params = new JsonObject()
      .put("name", "conversation");
    JsonRequest request = new GetPromptRequest(params).toRequest(1);

    sendRequest(HttpMethod.POST, request.toJson().toBuffer())
      .compose(resp -> resp.body())
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNull(response.getError(), "Should succeed");
        JsonObject result = (JsonObject) response.getResult();

        JsonArray messages = result.getJsonArray("messages");
        context.assertNotNull(messages, "Should have messages array");
        context.assertEquals(3, messages.size(), "Should have 3 messages");

        // Verify roles
        context.assertEquals("user", messages.getJsonObject(0).getString("role"));
        context.assertEquals("assistant", messages.getJsonObject(1).getString("role"));
        context.assertEquals("user", messages.getJsonObject(2).getString("role"));

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testUnsupportedPromptMethod(TestContext context) {

    Async async = context.async();

    JsonRequest request = JsonRequest.createRequest("prompts/unsupported", new JsonObject(), 1);

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
}
