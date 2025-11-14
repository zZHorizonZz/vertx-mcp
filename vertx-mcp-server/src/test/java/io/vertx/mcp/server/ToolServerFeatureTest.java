package io.vertx.mcp.server;

import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.json.schema.common.dsl.SchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;
import io.vertx.mcp.common.content.Content;
import io.vertx.mcp.common.content.TextContent;
import io.vertx.mcp.common.request.CallToolRequest;
import io.vertx.mcp.common.request.ListToolsRequest;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.server.impl.ToolServerFeature;
import org.junit.Test;

public class ToolServerFeatureTest extends ServerFeatureTestBase<ToolServerFeature> {

  // Common input schemas used across tests
  private static final SchemaBuilder EMPTY_SCHEMA = Schemas.objectSchema();
  private static final SchemaBuilder TEXT_INPUT_SCHEMA = Schemas.objectSchema()
    .requiredProperty("text", Schemas.stringSchema());
  private static final SchemaBuilder MESSAGE_INPUT_SCHEMA = Schemas.objectSchema()
    .requiredProperty("message", Schemas.stringSchema());
  private static final SchemaBuilder NUMBER_VALUE_SCHEMA = Schemas.objectSchema()
    .requiredProperty("value", Schemas.numberSchema());
  private static final SchemaBuilder STRING_VALUE_SCHEMA = Schemas.objectSchema()
    .requiredProperty("value", Schemas.stringSchema());

  // Common output schemas
  private static final SchemaBuilder MESSAGE_OUTPUT_SCHEMA = Schemas.objectSchema()
    .property("message", Schemas.stringSchema());
  private static final SchemaBuilder NUMBER_OUTPUT_SCHEMA = Schemas.objectSchema()
    .property("result", Schemas.numberSchema());

  @Override
  protected ToolServerFeature createFeature() {
    return new ToolServerFeature();
  }

  @Test
  public void testListToolsEmpty(TestContext context) {

    Async async = context.async();
    JsonRequest request = new ListToolsRequest().toRequest(1);

    sendRequest(HttpMethod.POST, request.toJson().toBuffer())
      .compose(resp -> resp.body())
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNull(response.getError(), "Should succeed");
        JsonObject result = (JsonObject) response.getResult();
        context.assertNotNull(result, "Should have result");

        JsonArray tools = result.getJsonArray("tools");
        context.assertNotNull(tools, "Should have tools array");
        context.assertEquals(0, tools.size(), "Should be empty");

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testListToolsWithStructuredTools(TestContext context) {

    // Add structured tools
    feature.addStructuredTool(
      "greet",
      "Greeting Tool",
      "Greets a person by name",
      StructuredToolHandler.create(
        Schemas.objectSchema()
          .requiredProperty("name", Schemas.stringSchema()),
        Schemas.objectSchema()
          .property("greeting", Schemas.stringSchema()),
        args ->
          Future.succeededFuture(new JsonObject().put("greeting", "Hello " + args.getString("name")))
      )
    );

    feature.addStructuredTool(
      "add",
      "Addition Tool",
      "Adds two numbers",
      StructuredToolHandler.create(
        Schemas.objectSchema()
          .requiredProperty("a", Schemas.numberSchema())
          .requiredProperty("b", Schemas.numberSchema()),
        Schemas.objectSchema()
          .property("result", Schemas.numberSchema()),
        args ->
          Future.succeededFuture(new JsonObject().put("result", args.getInteger("a") + args.getInteger("b")))
      )
    );


    Async async = context.async();
    JsonRequest request = new ListToolsRequest().toRequest(1);

    sendRequest(HttpMethod.POST, request.toJson().toBuffer())
      .compose(resp -> resp.body())
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNull(response.getError(), "Should succeed");
        JsonObject result = (JsonObject) response.getResult();

        JsonArray tools = result.getJsonArray("tools");
        context.assertNotNull(tools, "Should have tools array");
        context.assertEquals(2, tools.size(), "Should have 2 tools");

        // Find tools by name (order is not guaranteed from HashMap)
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

        // Verify greet tool
        context.assertNotNull(greetTool, "Should have greet tool");
        context.assertEquals("Greeting Tool", greetTool.getString("title"));
        context.assertEquals("Greets a person by name", greetTool.getString("description"));
        context.assertNotNull(greetTool.getJsonObject("inputSchema"));
        context.assertNotNull(greetTool.getJsonObject("outputSchema"));

        // Verify add tool
        context.assertNotNull(addTool, "Should have add tool");
        context.assertEquals("Addition Tool", addTool.getString("title"));
        context.assertEquals("Adds two numbers", addTool.getString("description"));

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testListToolsWithUnstructuredTools(TestContext context) {

    feature.addUnstructuredTool(
      "echo",
      "Echo Tool",
      "Echoes back the message",
      UnstructuredToolHandler.create(
        MESSAGE_INPUT_SCHEMA,
        args ->
          Future.succeededFuture(new Content[] {
            new TextContent(args.getString("message"))
          })
      )
    );


    Async async = context.async();
    JsonRequest request = new ListToolsRequest().toRequest(1);

    sendRequest(HttpMethod.POST, request.toJson().toBuffer())
      .compose(resp -> resp.body())
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNull(response.getError(), "Should succeed");
        JsonObject result = (JsonObject) response.getResult();

        JsonArray tools = result.getJsonArray("tools");
        context.assertEquals(1, tools.size(), "Should have 1 tool");

        JsonObject tool = tools.getJsonObject(0);
        context.assertEquals("echo", tool.getString("name"));
        context.assertEquals("Echo Tool", tool.getString("title"));
        context.assertNotNull(tool.getJsonObject("inputSchema"));

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testCallStructuredTool(TestContext context) {

    feature.addStructuredTool(
      "multiply",
      StructuredToolHandler.create(
        Schemas.objectSchema()
          .requiredProperty("x", Schemas.numberSchema())
          .requiredProperty("y", Schemas.numberSchema()),
        Schemas.objectSchema()
          .property("product", Schemas.numberSchema()),
        args -> {
          int x = args.getInteger("x");
          int y = args.getInteger("y");
          return Future.succeededFuture(new JsonObject().put("product", x * y));
        })
    );


    Async async = context.async();

    JsonObject params = new JsonObject()
      .put("name", "multiply")
      .put("arguments", new JsonObject()
        .put("x", 6)
        .put("y", 7));
    JsonRequest request = new CallToolRequest(params).toRequest(1);

    sendRequest(HttpMethod.POST, request.toJson().toBuffer())
      .compose(resp -> resp.body())
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNull(response.getError(), "Should succeed");
        JsonObject result = (JsonObject) response.getResult();
        context.assertNotNull(result, "Should have result");

        context.assertFalse(result.getBoolean("isError"), "Should not be error");
        JsonObject structuredContent = result.getJsonObject("structuredContent");
        context.assertNotNull(structuredContent, "Should have structured content");
        context.assertEquals(42, structuredContent.getInteger("product"));

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testCallUnstructuredTool(TestContext context) {

    feature.addUnstructuredTool(
      "uppercase",
      UnstructuredToolHandler.create(
        TEXT_INPUT_SCHEMA,
        args ->
          Future.succeededFuture(new Content[] {
            new TextContent(args.getString("text").toUpperCase())
          })
      )
    );


    Async async = context.async();

    JsonObject params = new JsonObject()
      .put("name", "uppercase")
      .put("arguments", new JsonObject()
        .put("text", "hello world"));
    JsonRequest request = new CallToolRequest(params).toRequest(1);

    sendRequest(HttpMethod.POST, request.toJson().toBuffer())
      .compose(resp -> resp.body())
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNull(response.getError(), "Should succeed");
        JsonObject result = (JsonObject) response.getResult();

        context.assertFalse(result.getBoolean("isError"), "Should not be error");
        JsonArray content = result.getJsonArray("content");
        context.assertNotNull(content, "Should have content");
        context.assertEquals(1, content.size());

        JsonObject textContent = content.getJsonObject(0);
        context.assertEquals("text", textContent.getString("type"));
        context.assertEquals("HELLO WORLD", textContent.getString("text"));

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testCallToolNotFound(TestContext context) {


    Async async = context.async();

    JsonObject params = new JsonObject()
      .put("name", "nonexistent")
      .put("arguments", new JsonObject());
    JsonRequest request = new CallToolRequest(params).toRequest(1);

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
  public void testCallToolMissingName(TestContext context) {


    Async async = context.async();

    JsonObject params = new JsonObject()
      .put("arguments", new JsonObject());
    JsonRequest request = JsonRequest.createRequest("tools/call", params, 1);

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
  public void testStructuredToolExecutionFailure(TestContext context) {

    feature.addStructuredTool(
      "failing-tool",
      StructuredToolHandler.create(
        NUMBER_VALUE_SCHEMA,
        NUMBER_OUTPUT_SCHEMA,
        args ->
          Future.failedFuture("Tool execution failed")
      )
    );


    Async async = context.async();

    JsonObject params = new JsonObject()
      .put("name", "failing-tool")
      .put("arguments", new JsonObject().put("value", 42));
    JsonRequest request = new CallToolRequest(params).toRequest(1);

    sendRequest(HttpMethod.POST, request.toJson().toBuffer())
      .compose(resp -> resp.body())
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNull(response.getError(), "Should not have JSON-RPC error");
        JsonObject result = (JsonObject) response.getResult();

        context.assertTrue(result.getBoolean("isError"), "Should be marked as error");
        JsonArray content = result.getJsonArray("content");
        context.assertNotNull(content);
        context.assertTrue(content.getJsonObject(0).getString("text").contains("Error:"));

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testUnstructuredToolExecutionFailure(TestContext context) {

    feature.addUnstructuredTool(
      "failing-unstructured",
      UnstructuredToolHandler.create(
        STRING_VALUE_SCHEMA,
        args ->
          Future.failedFuture("Unstructured tool failed")
      )
    );


    Async async = context.async();

    JsonObject params = new JsonObject()
      .put("name", "failing-unstructured")
      .put("arguments", new JsonObject().put("value", "test"));
    JsonRequest request = new CallToolRequest(params).toRequest(1);

    sendRequest(HttpMethod.POST, request.toJson().toBuffer())
      .compose(resp -> resp.body())
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNull(response.getError(), "Should not have JSON-RPC error");
        JsonObject result = (JsonObject) response.getResult();

        context.assertTrue(result.getBoolean("isError"), "Should be marked as error");
        JsonArray content = result.getJsonArray("content");
        context.assertTrue(content.getJsonObject(0).getString("text").contains("Error:"));

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testUnsupportedToolMethod(TestContext context) {


    Async async = context.async();

    JsonRequest request = JsonRequest.createRequest("tools/unsupported", new JsonObject(), 1);

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
  public void testCallToolWithNullArguments(TestContext context) {

    feature.addStructuredTool(
      "no-args-tool",
      StructuredToolHandler.create(
        EMPTY_SCHEMA,
        MESSAGE_OUTPUT_SCHEMA,
        args ->
          Future.succeededFuture(new JsonObject().put("message", "No arguments needed"))
      )
    );


    Async async = context.async();

    JsonObject params = new JsonObject()
      .put("name", "no-args-tool");
    // No arguments field
    JsonRequest request = new CallToolRequest(params).toRequest(1);

    sendRequest(HttpMethod.POST, request.toJson().toBuffer())
      .compose(resp -> resp.body())
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNull(response.getError(), "Should succeed");
        JsonObject result = (JsonObject) response.getResult();
        context.assertFalse(result.getBoolean("isError"));

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testMultipleTools(TestContext context) {

    // Add multiple tools
    for (int i = 0; i < 5; i++) {
      final int index = i;
      feature.addStructuredTool(
        "tool-" + i,
        StructuredToolHandler.create(
          EMPTY_SCHEMA,
          Schemas.objectSchema().property("index", Schemas.numberSchema()),
          args ->
            Future.succeededFuture(new JsonObject().put("index", index))
        )
      );
    }


    Async async = context.async();
    JsonRequest request = new ListToolsRequest().toRequest(1);

    sendRequest(HttpMethod.POST, request.toJson().toBuffer())
      .compose(resp -> resp.body())
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());

        context.assertNull(response.getError(), "Should succeed");
        JsonObject result = (JsonObject) response.getResult();

        JsonArray tools = result.getJsonArray("tools");
        context.assertEquals(5, tools.size(), "Should have 5 tools");

        async.complete();
      }));

    async.awaitSuccess(10_000);
  }
}
