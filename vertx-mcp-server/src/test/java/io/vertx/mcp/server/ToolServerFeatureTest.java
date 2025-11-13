package io.vertx.mcp.server;

import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.json.schema.common.dsl.Schemas;
import io.vertx.mcp.common.content.Content;
import io.vertx.mcp.common.content.TextContent;
import io.vertx.mcp.common.request.CallToolRequest;
import io.vertx.mcp.common.request.ListToolsRequest;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.server.impl.ToolServerFeature;
import org.junit.Test;

public class ToolServerFeatureTest extends HttpTransportTestBase {

  @Test
  public void testListToolsEmpty(TestContext context) {
    ModelContextProtocolServer server = ModelContextProtocolServer.create();
    ToolServerFeature toolFeature = new ToolServerFeature();
    server.serverFeatures(toolFeature);

    startServer(context, server);

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
    ModelContextProtocolServer server = ModelContextProtocolServer.create();
    ToolServerFeature toolFeature = new ToolServerFeature();

    // Add structured tools
    toolFeature.addStructuredTool(
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

    toolFeature.addStructuredTool(
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

    server.serverFeatures(toolFeature);
    startServer(context, server);

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
    ModelContextProtocolServer server = ModelContextProtocolServer.create();
    ToolServerFeature toolFeature = new ToolServerFeature();

    toolFeature.addUnstructuredTool(
      "echo",
      "Echo Tool",
      "Echoes back the message",
      UnstructuredToolHandler.create(
        Schemas.objectSchema()
          .requiredProperty("message", Schemas.stringSchema()),
        args ->
          Future.succeededFuture(new Content[] {
            new TextContent(args.getString("message"))
          })
      )
    );

    server.serverFeatures(toolFeature);
    startServer(context, server);

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
    ModelContextProtocolServer server = ModelContextProtocolServer.create();
    ToolServerFeature toolFeature = new ToolServerFeature();

    toolFeature.addStructuredTool(
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

    server.serverFeatures(toolFeature);
    startServer(context, server);

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
    ModelContextProtocolServer server = ModelContextProtocolServer.create();
    ToolServerFeature toolFeature = new ToolServerFeature();

    toolFeature.addUnstructuredTool(
      "uppercase",
      UnstructuredToolHandler.create(
        Schemas.objectSchema()
          .requiredProperty("text", Schemas.stringSchema()),
        args ->
          Future.succeededFuture(new Content[] {
            new TextContent(args.getString("text").toUpperCase())
          })
      )
    );

    server.serverFeatures(toolFeature);
    startServer(context, server);

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
    ModelContextProtocolServer server = ModelContextProtocolServer.create();
    ToolServerFeature toolFeature = new ToolServerFeature();
    server.serverFeatures(toolFeature);

    startServer(context, server);

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
    ModelContextProtocolServer server = ModelContextProtocolServer.create();
    ToolServerFeature toolFeature = new ToolServerFeature();
    server.serverFeatures(toolFeature);

    startServer(context, server);

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
    ModelContextProtocolServer server = ModelContextProtocolServer.create();
    ToolServerFeature toolFeature = new ToolServerFeature();

    toolFeature.addStructuredTool(
      "failing-tool",
      StructuredToolHandler.create(
        Schemas.objectSchema()
          .requiredProperty("value", Schemas.numberSchema()),
        Schemas.objectSchema()
          .property("result", Schemas.numberSchema()),
        args ->
          Future.failedFuture("Tool execution failed")
      )
    );

    server.serverFeatures(toolFeature);
    startServer(context, server);

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
    ModelContextProtocolServer server = ModelContextProtocolServer.create();
    ToolServerFeature toolFeature = new ToolServerFeature();

    toolFeature.addUnstructuredTool(
      "failing-unstructured",
      UnstructuredToolHandler.create(
        Schemas.objectSchema()
          .requiredProperty("value", Schemas.stringSchema()),
        args ->
          Future.failedFuture("Unstructured tool failed")
      )
    );

    server.serverFeatures(toolFeature);
    startServer(context, server);

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
    ModelContextProtocolServer server = ModelContextProtocolServer.create();
    ToolServerFeature toolFeature = new ToolServerFeature();
    server.serverFeatures(toolFeature);

    startServer(context, server);

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
    ModelContextProtocolServer server = ModelContextProtocolServer.create();
    ToolServerFeature toolFeature = new ToolServerFeature();

    toolFeature.addStructuredTool(
      "no-args-tool",
      StructuredToolHandler.create(
        Schemas.objectSchema(),
        Schemas.objectSchema()
          .property("message", Schemas.stringSchema()),
        args ->
          Future.succeededFuture(new JsonObject().put("message", "No arguments needed"))
      )
    );

    server.serverFeatures(toolFeature);
    startServer(context, server);

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
    ModelContextProtocolServer server = ModelContextProtocolServer.create();
    ToolServerFeature toolFeature = new ToolServerFeature();

    // Add multiple tools
    for (int i = 0; i < 5; i++) {
      final int index = i;
      toolFeature.addStructuredTool(
        "tool-" + i,
        StructuredToolHandler.create(
          Schemas.objectSchema(),
          Schemas.objectSchema()
            .property("index", Schemas.numberSchema()),
          args ->
            Future.succeededFuture(new JsonObject().put("index", index))
        )
      );
    }

    server.serverFeatures(toolFeature);
    startServer(context, server);

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
