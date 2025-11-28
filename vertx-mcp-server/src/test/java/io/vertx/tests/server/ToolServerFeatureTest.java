package io.vertx.tests.server;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;
import io.vertx.mcp.common.content.Content;
import io.vertx.mcp.common.content.TextContent;
import io.vertx.mcp.common.request.CallToolRequest;
import io.vertx.mcp.common.request.ListToolsRequest;
import io.vertx.mcp.common.rpc.JsonError;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.server.feature.ToolServerFeature;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class ToolServerFeatureTest extends ServerFeatureTestBase<ToolServerFeature> {

  private static final ObjectSchemaBuilder EMPTY_SCHEMA = Schemas.objectSchema();
  private static final ObjectSchemaBuilder TEXT_INPUT_SCHEMA = Schemas.objectSchema().requiredProperty("text", Schemas.stringSchema());
  private static final ObjectSchemaBuilder MESSAGE_INPUT_SCHEMA = Schemas.objectSchema().requiredProperty("message", Schemas.stringSchema());
  private static final ObjectSchemaBuilder NUMBER_VALUE_SCHEMA = Schemas.objectSchema().requiredProperty("value", Schemas.numberSchema());
  private static final ObjectSchemaBuilder STRING_VALUE_SCHEMA = Schemas.objectSchema().requiredProperty("value", Schemas.stringSchema());

  private static final ObjectSchemaBuilder MESSAGE_OUTPUT_SCHEMA = Schemas.objectSchema().property("message", Schemas.stringSchema());
  private static final ObjectSchemaBuilder NUMBER_OUTPUT_SCHEMA = Schemas.objectSchema().property("result", Schemas.numberSchema());

  @Override
  protected ToolServerFeature createFeature() {
    return new ToolServerFeature();
  }

  @Test
  public void testListToolsEmpty(TestContext context) throws Throwable {
    JsonResponse response = sendRequest(HttpMethod.POST, new ListToolsRequest())
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .expecting(JsonResponse::isSuccess)
      .await(10, TimeUnit.SECONDS);

    JsonObject result = (JsonObject) response.getResult();

    context.assertNotNull(result, "Should have result");

    JsonArray tools = result.getJsonArray("tools");
    context.assertNotNull(tools, "Should have tools array");
    context.assertEquals(0, tools.size(), "Should be empty");
  }

  @Test
  public void testListToolsWithStructuredTools(TestContext context) throws Throwable {
    feature.addStructuredTool(
      "greet",
      "Greeting Tool",
      "Greets a person by name",
      Schemas.objectSchema().requiredProperty("name", Schemas.stringSchema()),
      Schemas.objectSchema().property("greeting", Schemas.stringSchema()),
      args -> Future.succeededFuture(new JsonObject().put("greeting", "Hello " + args.getString("name")))
    );

    feature.addStructuredTool(
      "add",
      "Addition Tool",
      "Adds two numbers",
      Schemas.objectSchema()
        .requiredProperty("a", Schemas.numberSchema())
        .requiredProperty("b", Schemas.numberSchema()),
      Schemas.objectSchema().property("result", Schemas.numberSchema()),
      args -> Future.succeededFuture(new JsonObject().put("result", args.getInteger("a") + args.getInteger("b")))
    );

    JsonResponse response = sendRequest(HttpMethod.POST, new ListToolsRequest())
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .expecting(JsonResponse::isSuccess)
      .await(10, TimeUnit.SECONDS);

    JsonObject result = (JsonObject) response.getResult();

    JsonArray tools = result.getJsonArray("tools");
    context.assertNotNull(tools, "Should have tools array");
    context.assertEquals(2, tools.size(), "Should have 2 tools");

    JsonObject greetTool = null;
    JsonObject addTool = null;
    for (int i = 0; i < tools.size(); i++) {
      JsonObject tool = tools.getJsonObject(i);
      if ("greet".equals(tool.getString("name"))) {
        greetTool = tool;
      } else if ("add".equals(tool.getString("name"))) {
        addTool = tool;
      }
    }

    context.assertNotNull(greetTool, "Should have greet tool");
    context.assertEquals("Greeting Tool", greetTool.getString("title"));
    context.assertEquals("Greets a person by name", greetTool.getString("description"));
    context.assertNotNull(greetTool.getJsonObject("inputSchema"));
    context.assertNotNull(greetTool.getJsonObject("outputSchema"));

    context.assertNotNull(addTool, "Should have add tool");
    context.assertEquals("Addition Tool", addTool.getString("title"));
    context.assertEquals("Adds two numbers", addTool.getString("description"));
  }

  @Test
  public void testListToolsWithUnstructuredTools(TestContext context) throws Throwable {
    feature.addUnstructuredTool(
      "echo",
      "Echo Tool",
      "Echoes back the message",
      MESSAGE_INPUT_SCHEMA,
      args -> Future.succeededFuture(new Content[] {
        new TextContent(args.getString("message"))
      })
    );

    JsonResponse response = sendRequest(HttpMethod.POST, new ListToolsRequest())
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .expecting(JsonResponse::isSuccess)
      .await(10, TimeUnit.SECONDS);

    JsonObject result = (JsonObject) response.getResult();

    JsonArray tools = result.getJsonArray("tools");
    context.assertEquals(1, tools.size(), "Should have 1 tool");

    JsonObject tool = tools.getJsonObject(0);
    context.assertEquals("echo", tool.getString("name"));
    context.assertEquals("Echo Tool", tool.getString("title"));
    context.assertNotNull(tool.getJsonObject("inputSchema"));
  }

  @Test
  public void testCallStructuredTool(TestContext context) throws Throwable {
    feature.addStructuredTool(
      "multiply",
      Schemas.objectSchema()
        .requiredProperty("x", Schemas.numberSchema())
        .requiredProperty("y", Schemas.numberSchema()),
      Schemas.objectSchema().property("product", Schemas.numberSchema()),
      args -> {
        int x = args.getInteger("x");
        int y = args.getInteger("y");
        return Future.succeededFuture(new JsonObject().put("product", x * y));
      }
    );

    JsonObject params = new JsonObject().put("name", "multiply").put("arguments", new JsonObject().put("x", 6).put("y", 7));

    JsonResponse response = sendRequest(HttpMethod.POST, new CallToolRequest(params))
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .expecting(JsonResponse::isSuccess)
      .await(10, TimeUnit.SECONDS);

    JsonObject result = (JsonObject) response.getResult();
    context.assertNotNull(result, "Should have result");

    context.assertFalse(result.getBoolean("isError"), "Should not be error");
    JsonObject structuredContent = result.getJsonObject("structuredContent");
    context.assertNotNull(structuredContent, "Should have structured content");
    context.assertEquals(42, structuredContent.getInteger("product"));
  }

  @Test
  public void testCallUnstructuredTool(TestContext context) throws Throwable {
    feature.addUnstructuredTool(
      "uppercase",
      TEXT_INPUT_SCHEMA,
      args -> Future.succeededFuture(new Content[] {
        new TextContent(args.getString("text").toUpperCase())
      })
    );

    JsonObject params = new JsonObject().put("name", "uppercase").put("arguments", new JsonObject().put("text", "hello world"));

    JsonResponse response = sendRequest(HttpMethod.POST, new CallToolRequest(params))
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .expecting(JsonResponse::isSuccess)
      .await(10, TimeUnit.SECONDS);

    JsonObject result = (JsonObject) response.getResult();

    context.assertFalse(result.getBoolean("isError"), "Should not be error");
    JsonArray content = result.getJsonArray("content");
    context.assertNotNull(content, "Should have content");
    context.assertEquals(1, content.size());

    JsonObject textContent = content.getJsonObject(0);
    context.assertEquals("text", textContent.getString("type"));
    context.assertEquals("HELLO WORLD", textContent.getString("text"));
  }

  @Test
  public void testCallToolNotFound(TestContext context) throws Throwable {
    JsonObject params = new JsonObject().put("name", "nonexistent").put("arguments", new JsonObject());

    JsonResponse response = sendRequest(HttpMethod.POST, new CallToolRequest(params))
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .await(10, TimeUnit.SECONDS);

    context.assertNotNull(response.getError(), "Should have error");
    context.assertEquals(JsonError.INVALID_PARAMS, response.getError().getCode(), "Should be invalid params");
    context.assertTrue(response.getError().getMessage().contains("not found"));
  }

  @Test
  public void testCallToolMissingName(TestContext context) throws Throwable {
    JsonObject params = new JsonObject().put("arguments", new JsonObject());

    JsonResponse response = sendRequest(HttpMethod.POST, JsonRequest.createRequest("tools/call", params, 1))
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .await(10, TimeUnit.SECONDS);

    context.assertNotNull(response.getError(), "Should have error");
    context.assertEquals(JsonError.INVALID_PARAMS, response.getError().getCode(), "Should be invalid params");
    context.assertTrue(response.getError().getMessage().contains("Missing 'name'"));
  }

  @Test
  public void testStructuredToolExecutionFailure(TestContext context) throws Throwable {
    feature.addStructuredTool(
      "failing-tool",
      NUMBER_VALUE_SCHEMA,
      NUMBER_OUTPUT_SCHEMA,
      args -> Future.failedFuture("Tool execution failed")
    );

    JsonObject params = new JsonObject().put("name", "failing-tool").put("arguments", new JsonObject().put("value", 42));

    JsonResponse response = sendRequest(HttpMethod.POST, new CallToolRequest(params))
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .expecting(JsonResponse::isSuccess)
      .await(10, TimeUnit.SECONDS);

    JsonObject result = (JsonObject) response.getResult();

    context.assertTrue(result.getBoolean("isError"), "Should be marked as error");
    JsonArray content = result.getJsonArray("content");
    context.assertNotNull(content);
    context.assertTrue(content.getJsonObject(0).getString("text").contains("Error:"));
  }

  @Test
  public void testUnstructuredToolExecutionFailure(TestContext context) throws Throwable {
    feature.addUnstructuredTool(
      "failing-unstructured",
      STRING_VALUE_SCHEMA,
      args -> Future.failedFuture("Unstructured tool failed")
    );

    JsonObject params = new JsonObject().put("name", "failing-unstructured").put("arguments", new JsonObject().put("value", "test"));

    JsonResponse response = sendRequest(HttpMethod.POST, new CallToolRequest(params))
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .expecting(JsonResponse::isSuccess)
      .await(10, TimeUnit.SECONDS);

    JsonObject result = (JsonObject) response.getResult();

    context.assertTrue(result.getBoolean("isError"), "Should be marked as error");
    JsonArray content = result.getJsonArray("content");
    context.assertTrue(content.getJsonObject(0).getString("text").contains("Error:"));
  }

  @Test
  public void testUnsupportedToolMethod(TestContext context) throws Throwable {
    JsonResponse response = sendRequest(HttpMethod.POST, JsonRequest.createRequest("tools/unsupported", new JsonObject(), 1))
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .await(10, TimeUnit.SECONDS);

    context.assertNotNull(response.getError(), "Should have error");
    context.assertEquals(JsonError.METHOD_NOT_FOUND, response.getError().getCode(), "Should be method not found");
  }

  @Test
  public void testCallToolWithNullArguments(TestContext context) throws Throwable {
    feature.addStructuredTool(
      "no-args-tool",
      EMPTY_SCHEMA,
      MESSAGE_OUTPUT_SCHEMA,
      args -> Future.succeededFuture(new JsonObject().put("message", "No arguments needed"))
    );

    JsonObject params = new JsonObject().put("name", "no-args-tool");

    JsonResponse response = sendRequest(HttpMethod.POST, new CallToolRequest(params))
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .expecting(JsonResponse::isSuccess)
      .await(10, TimeUnit.SECONDS);

    JsonObject result = (JsonObject) response.getResult();
    context.assertFalse(result.getBoolean("isError"));
  }

  @Test
  public void testMultipleTools(TestContext context) throws Throwable {
    for (int i = 0; i < 5; i++) {
      final int index = i;
      feature.addStructuredTool(
        "tool-" + i,
        EMPTY_SCHEMA,
        Schemas.objectSchema().property("index", Schemas.numberSchema()),
        args -> Future.succeededFuture(new JsonObject().put("index", index))
      );
    }

    JsonResponse response = sendRequest(HttpMethod.POST, new ListToolsRequest())
      .compose(HttpClientResponse::body)
      .map(body -> JsonResponse.fromJson(body.toJsonObject()))
      .expecting(JsonResponse::isSuccess)
      .await(10, TimeUnit.SECONDS);

    JsonObject result = (JsonObject) response.getResult();

    JsonArray tools = result.getJsonArray("tools");
    context.assertEquals(5, tools.size(), "Should have 5 tools");
  }
}
