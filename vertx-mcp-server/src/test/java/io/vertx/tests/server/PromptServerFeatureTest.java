package io.vertx.tests.server;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.json.schema.common.dsl.ArraySchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;
import io.vertx.mcp.common.content.TextContent;
import io.vertx.mcp.common.prompt.Prompt;
import io.vertx.mcp.common.prompt.PromptMessage;
import io.vertx.mcp.common.request.GetPromptRequest;
import io.vertx.mcp.common.request.ListPromptsRequest;
import io.vertx.mcp.common.result.ListPromptsResult;
import io.vertx.mcp.common.rpc.JsonError;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.server.feature.PromptServerFeature;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class PromptServerFeatureTest extends ServerFeatureTestBase<PromptServerFeature> {

  private static final ArraySchemaBuilder CODE_ARGUMENT_SCHEMA = Schemas.arraySchema().items(Schemas.objectSchema().requiredProperty("code", Schemas.stringSchema()));

  @Override
  protected PromptServerFeature createFeature() {
    return new PromptServerFeature();
  }

  @Test
  public void testListPromptsEmpty(TestContext context) throws Throwable {
    JsonResponse response = sendRequest(HttpMethod.POST, new ListPromptsRequest())
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .expecting(JsonResponse::isSuccess)
      .await(10, TimeUnit.SECONDS);

    ListPromptsResult result = new ListPromptsResult((JsonObject) response.getResult());

    context.assertNotNull(result.getPrompts(), "Should have prompts array");
    context.assertEquals(0, result.getPrompts().size(), "Should be empty");
  }

  @Test
  public void testListPromptsWithPrompts(TestContext context) throws Throwable {
    feature.addPrompt(
      "code_review",
      "Code Review",
      "Reviews code and suggests improvements",
      CODE_ARGUMENT_SCHEMA,
      args -> {
        List<PromptMessage> messages = new ArrayList<>();
        PromptMessage message = new PromptMessage()
          .setRole("user")
          .setContent(new TextContent("Review this code: " + args.getString("code")).toJson());
        messages.add(message);
        return Future.succeededFuture(messages);
      }
    );

    feature.addPrompt(
      "explain_code",
      "Explain Code",
      "Explains what code does",
      CODE_ARGUMENT_SCHEMA,
      args -> {
        List<PromptMessage> messages = new ArrayList<>();
        PromptMessage message = new PromptMessage()
          .setRole("user")
          .setContent(new TextContent("Explain this code: " + args.getString("code")).toJson());
        messages.add(message);
        return Future.succeededFuture(messages);
      }
    );

    JsonResponse response = sendRequest(HttpMethod.POST, new ListPromptsRequest())
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .expecting(JsonResponse::isSuccess)
      .await(10, TimeUnit.SECONDS);

    ListPromptsResult result = new ListPromptsResult((JsonObject) response.getResult());

    context.assertNotNull(result.getPrompts(), "Should have prompts list");
    context.assertEquals(2, result.getPrompts().size(), "Should have 2 prompts");

    Optional<Prompt> codeReview = result.getPrompts().stream().filter(p -> "code_review".equals(p.getName())).findFirst();
    context.assertTrue(codeReview.isPresent(), "Should have code_review prompt");
    context.assertEquals("Code Review", codeReview.get().getTitle());
    context.assertEquals("Reviews code and suggests improvements", codeReview.get().getDescription());

    Optional<Prompt> explainCode = result.getPrompts().stream().filter(p -> "explain_code".equals(p.getName())).findFirst();
    context.assertTrue(explainCode.isPresent(), "Should have explain_code prompt");
    context.assertEquals("Explain Code", explainCode.get().getTitle());
    context.assertEquals("Explains what code does", explainCode.get().getDescription());
  }

  @Test
  public void testGetPrompt(TestContext context) throws Throwable {
    feature.addPrompt(
      "code_review",
      "Code Review",
      "Reviews code and suggests improvements",
      CODE_ARGUMENT_SCHEMA,
      args -> {
        String code = args.getString("code");
        List<PromptMessage> messages = new ArrayList<>();

        PromptMessage message = new PromptMessage()
          .setRole("user")
          .setContent(new TextContent("Please review this code:\n" + code).toJson());
        messages.add(message);

        return Future.succeededFuture(messages);
      }
    );

    JsonResponse response = sendRequest(HttpMethod.POST, new GetPromptRequest(
      new JsonObject().put("name", "code_review").put("arguments", new JsonObject().put("code", "def hello():\n    print('world')")))
    )
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .expecting(JsonResponse::isSuccess)
      .await(10, TimeUnit.SECONDS);

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
  }

  @Test
  public void testGetPromptNotFound(TestContext context) throws Throwable {
    JsonResponse response = sendRequest(HttpMethod.POST, new GetPromptRequest(new JsonObject().put("name", "nonexistent")))
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .await(10, TimeUnit.SECONDS);

    context.assertNotNull(response.getError(), "Should have error");
    context.assertEquals(JsonError.INVALID_PARAMS, response.getError().getCode(), "Should be invalid params");
    context.assertTrue(response.getError().getMessage().contains("not found"));
  }

  @Test
  public void testGetPromptMissingName(TestContext context) throws Throwable {
    JsonResponse response = sendRequest(HttpMethod.POST, JsonRequest.createRequest("prompts/get", new JsonObject(), 1))
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .await(10, TimeUnit.SECONDS);

    context.assertNotNull(response.getError(), "Should have error");
    context.assertEquals(JsonError.INVALID_PARAMS, response.getError().getCode(), "Should be invalid params");
    context.assertTrue(response.getError().getMessage().contains("Missing 'name'"));
  }

  @Test
  public void testPromptHandlerFailure(TestContext context) throws Throwable {
    feature.addPrompt(
      "failing_prompt",
      "Failing Prompt",
      "A prompt that fails",
      null,
      args -> Future.failedFuture("Prompt generation failed")
    );

    JsonResponse response = sendRequest(HttpMethod.POST, new GetPromptRequest(new JsonObject().put("name", "failing_prompt")))
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .await(10, TimeUnit.SECONDS);

    context.assertNotNull(response.getError(), "Should have error");
    context.assertEquals(JsonError.INTERNAL_ERROR, response.getError().getCode(), "Should be internal error");
    context.assertTrue(response.getError().getMessage().contains("failed"));
  }

  @Test
  public void testGetPromptWithMultipleMessages(TestContext context) throws Throwable {
    feature.addPrompt(
      "conversation",
      "Conversation",
      "A multi-turn conversation prompt",
      null,
      args -> {
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
      }
    );

    JsonResponse response = sendRequest(HttpMethod.POST, new GetPromptRequest(new JsonObject().put("name", "conversation")))
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .expecting(JsonResponse::isSuccess)
      .await(10, TimeUnit.SECONDS);

    JsonObject result = (JsonObject) response.getResult();

    JsonArray messages = result.getJsonArray("messages");
    context.assertNotNull(messages, "Should have messages array");
    context.assertEquals(3, messages.size(), "Should have 3 messages");

    context.assertEquals("user", messages.getJsonObject(0).getString("role"));
    context.assertEquals("assistant", messages.getJsonObject(1).getString("role"));
    context.assertEquals("user", messages.getJsonObject(2).getString("role"));
  }

  @Test
  public void testUnsupportedPromptMethod(TestContext context) throws Throwable {
    JsonResponse response = sendRequest(HttpMethod.POST, JsonRequest.createRequest("prompts/unsupported", new JsonObject(), 1))
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .await(10, TimeUnit.SECONDS);

    context.assertNotNull(response.getError(), "Should have error");
    context.assertEquals(JsonError.METHOD_NOT_FOUND, response.getError().getCode(), "Should be method not found");
  }
}
