package io.vertx.mcp.it;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;
import io.vertx.mcp.client.ClientRequestException;
import io.vertx.mcp.common.content.Content;
import io.vertx.mcp.common.content.TextContent;
import io.vertx.mcp.common.request.CallToolRequest;
import io.vertx.mcp.common.request.ListToolsRequest;
import io.vertx.mcp.common.result.CallToolResult;
import io.vertx.mcp.common.result.ListToolsResult;
import io.vertx.mcp.common.rpc.JsonError;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.tool.Tool;
import io.vertx.mcp.server.ModelContextProtocolServer;
import io.vertx.mcp.server.feature.ToolServerFeature;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class ToolServerFeatureTest extends HttpTransportTestBase {

  private static final ObjectSchemaBuilder EMPTY_SCHEMA = Schemas.objectSchema();
  private static final ObjectSchemaBuilder TEXT_INPUT_SCHEMA = Schemas.objectSchema().requiredProperty("text", Schemas.stringSchema());
  private static final ObjectSchemaBuilder MESSAGE_INPUT_SCHEMA = Schemas.objectSchema().requiredProperty("message", Schemas.stringSchema());
  private static final ObjectSchemaBuilder NUMBER_VALUE_SCHEMA = Schemas.objectSchema().requiredProperty("value", Schemas.numberSchema());
  private static final ObjectSchemaBuilder STRING_VALUE_SCHEMA = Schemas.objectSchema().requiredProperty("value", Schemas.stringSchema());

  private static final ObjectSchemaBuilder MESSAGE_OUTPUT_SCHEMA = Schemas.objectSchema().property("message", Schemas.stringSchema());
  private static final ObjectSchemaBuilder NUMBER_OUTPUT_SCHEMA = Schemas.objectSchema().property("result", Schemas.numberSchema());

  private ToolServerFeature toolFeature;

  @Before
  public void setUpFeatures(TestContext context) {
    ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx);
    super.startServer(context, server);

    toolFeature = new ToolServerFeature();
    server.addServerFeature(toolFeature);
  }

  @Test
  public void testListToolsEmpty(TestContext context) throws Throwable {
    ListToolsResult result = (ListToolsResult) getClient().sendRequest(new ListToolsRequest())
      .expecting(r -> r instanceof ListToolsResult)
      .await(10, TimeUnit.SECONDS);

    context.assertNotNull(result, "Should have result");

    List<Tool> tools = result.getTools();
    context.assertNotNull(tools, "Should have tools list");
    context.assertEquals(0, tools.size(), "Should be empty");
  }

  @Test
  public void testListToolsWithStructuredTools(TestContext context) throws Throwable {
    toolFeature.addStructuredTool(
      "greet",
      "Greeting Tool",
      "Greets a person by name",
      Schemas.objectSchema().requiredProperty("name", Schemas.stringSchema()),
      Schemas.objectSchema().property("greeting", Schemas.stringSchema()),
      args -> Future.succeededFuture(new JsonObject().put("greeting", "Hello " + args.getString("name")))
    );

    toolFeature.addStructuredTool(
      "add",
      "Addition Tool",
      "Adds two numbers",
      Schemas.objectSchema()
        .requiredProperty("a", Schemas.numberSchema())
        .requiredProperty("b", Schemas.numberSchema()),
      Schemas.objectSchema().property("result", Schemas.numberSchema()),
      args -> Future.succeededFuture(new JsonObject().put("result", args.getInteger("a") + args.getInteger("b")))
    );

    ListToolsResult result = (ListToolsResult) getClient().sendRequest(new ListToolsRequest())
      .expecting(r -> r instanceof ListToolsResult)
      .await(10, TimeUnit.SECONDS);

    List<Tool> tools = result.getTools();
    context.assertNotNull(tools, "Should have tools list");
    context.assertEquals(2, tools.size(), "Should have 2 tools");

    Tool greetTool = null;
    Tool addTool = null;
    for (Tool tool : tools) {
      if ("greet".equals(tool.getName())) {
        greetTool = tool;
      } else if ("add".equals(tool.getName())) {
        addTool = tool;
      }
    }

    context.assertNotNull(greetTool, "Should have greet tool");
    context.assertEquals("Greeting Tool", greetTool.getTitle());
    context.assertEquals("Greets a person by name", greetTool.getDescription());
    context.assertNotNull(greetTool.getInputSchema());
    context.assertNotNull(greetTool.getOutputSchema());

    context.assertNotNull(addTool, "Should have add tool");
    context.assertEquals("Addition Tool", addTool.getTitle());
    context.assertEquals("Adds two numbers", addTool.getDescription());
  }

  @Test
  public void testListToolsWithUnstructuredTools(TestContext context) throws Throwable {
    toolFeature.addUnstructuredTool(
      "echo",
      "Echo Tool",
      "Echoes back the message",
      MESSAGE_INPUT_SCHEMA,
      args -> Future.succeededFuture(new Content[] {
        new TextContent(args.getString("message"))
      })
    );

    ListToolsResult result = (ListToolsResult) getClient().sendRequest(new ListToolsRequest())
      .expecting(r -> r instanceof ListToolsResult)
      .await(10, TimeUnit.SECONDS);

    List<Tool> tools = result.getTools();
    context.assertEquals(1, tools.size(), "Should have 1 tool");

    Tool tool = tools.get(0);
    context.assertEquals("echo", tool.getName());
    context.assertEquals("Echo Tool", tool.getTitle());
    context.assertNotNull(tool.getInputSchema());
  }

  @Test
  public void testCallStructuredTool(TestContext context) throws Throwable {
    toolFeature.addStructuredTool(
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

    CallToolResult result = (CallToolResult) getClient().sendRequest(new CallToolRequest(params))
      .expecting(r -> r instanceof CallToolResult)
      .await(10, TimeUnit.SECONDS);

    context.assertNotNull(result, "Should have result");

    context.assertFalse(result.getIsError(), "Should not be error");
    JsonObject structuredContent = result.getStructuredContent();
    context.assertNotNull(structuredContent, "Should have structured content");
    context.assertEquals(42, structuredContent.getInteger("product"));
  }

  @Test
  public void testCallUnstructuredTool(TestContext context) throws Throwable {
    toolFeature.addUnstructuredTool(
      "uppercase",
      TEXT_INPUT_SCHEMA,
      args -> Future.succeededFuture(new Content[] {
        new TextContent(args.getString("text").toUpperCase())
      })
    );

    JsonObject params = new JsonObject().put("name", "uppercase").put("arguments", new JsonObject().put("text", "hello world"));

    CallToolResult result = (CallToolResult) getClient().sendRequest(new CallToolRequest(params))
      .expecting(r -> r instanceof CallToolResult)
      .await(10, TimeUnit.SECONDS);

    context.assertFalse(result.getIsError(), "Should not be error");
    JsonArray content = result.getContent();
    context.assertNotNull(content, "Should have content");
    context.assertEquals(1, content.size());

    JsonObject textContent = content.getJsonObject(0);
    context.assertEquals("text", textContent.getString("type"));
    context.assertEquals("HELLO WORLD", textContent.getString("text"));
  }

  @Test
  public void testCallToolNotFound(TestContext context) throws Throwable {
    JsonObject params = new JsonObject().put("name", "nonexistent").put("arguments", new JsonObject());

    try {
      getClient().sendRequest(new CallToolRequest(params))
        .await(10, TimeUnit.SECONDS);
      context.fail("Should have thrown ClientRequestException");
    } catch (ClientRequestException e) {
      context.assertEquals(JsonError.INVALID_PARAMS, e.getCode(), "Should be invalid params");
      context.assertTrue(e.getMessage().contains("not found"));
    }
  }

  @Test
  public void testCallToolMissingName(TestContext context) throws Throwable {
    JsonObject params = new JsonObject().put("arguments", new JsonObject());

    try {
      createSession()
        .compose(session -> session.sendRequest(JsonRequest.createRequest("tools/call", params, 1)))
        .await(10, TimeUnit.SECONDS);
      context.fail("Should have thrown ClientRequestException");
    } catch (ClientRequestException e) {
      context.assertEquals(JsonError.INVALID_PARAMS, e.getCode(), "Should be invalid params");
      context.assertTrue(e.getMessage().contains("Missing 'name'"));
    }
  }

  @Test
  public void testStructuredToolExecutionFailure(TestContext context) throws Throwable {
    toolFeature.addStructuredTool(
      "failing-tool",
      NUMBER_VALUE_SCHEMA,
      NUMBER_OUTPUT_SCHEMA,
      args -> Future.failedFuture("Tool execution failed")
    );

    JsonObject params = new JsonObject().put("name", "failing-tool").put("arguments", new JsonObject().put("value", 42));
    CallToolResult result = (CallToolResult) getClient().sendRequest(new CallToolRequest(params)).await(10, TimeUnit.SECONDS);

    context.assertTrue(result.getIsError());
  }

  @Test
  public void testUnstructuredToolExecutionFailure(TestContext context) throws Throwable {
    toolFeature.addUnstructuredTool(
      "failing-unstructured",
      STRING_VALUE_SCHEMA,
      args -> Future.failedFuture("Unstructured tool failed")
    );

    JsonObject params = new JsonObject().put("name", "failing-unstructured").put("arguments", new JsonObject().put("value", "test"));
    CallToolResult result = (CallToolResult) getClient().sendRequest(new CallToolRequest(params)).await(10, TimeUnit.SECONDS);

    context.assertTrue(result.getIsError());
  }

  @Test
  public void testUnsupportedToolMethod(TestContext context) throws Throwable {
    try {
      createSession()
        .compose(session -> session.sendRequest(JsonRequest.createRequest("tools/unsupported", new JsonObject(), 1)))
        .await(10, TimeUnit.SECONDS);
      context.fail("Should have thrown ClientRequestException");
    } catch (ClientRequestException e) {
      context.assertEquals(JsonError.METHOD_NOT_FOUND, e.getCode(), "Should be method not found");
    }
  }

  @Test
  public void testCallToolWithNullArguments(TestContext context) throws Throwable {
    toolFeature.addStructuredTool(
      "no-args-tool",
      EMPTY_SCHEMA,
      MESSAGE_OUTPUT_SCHEMA,
      args -> Future.succeededFuture(new JsonObject().put("message", "No arguments needed"))
    );

    JsonObject params = new JsonObject().put("name", "no-args-tool");

    CallToolResult result = (CallToolResult) getClient().sendRequest(new CallToolRequest(params))
      .expecting(r -> r instanceof CallToolResult)
      .await(10, TimeUnit.SECONDS);

    context.assertFalse(result.getIsError());
  }

  @Test
  public void testMultipleTools(TestContext context) throws Throwable {
    for (int i = 0; i < 5; i++) {
      final int index = i;
      toolFeature.addStructuredTool(
        "tool-" + i,
        EMPTY_SCHEMA,
        Schemas.objectSchema().property("index", Schemas.numberSchema()),
        args -> Future.succeededFuture(new JsonObject().put("index", index))
      );
    }

    ListToolsResult result = (ListToolsResult) getClient().sendRequest(new ListToolsRequest())
      .expecting(r -> r instanceof ListToolsResult)
      .await(10, TimeUnit.SECONDS);

    List<Tool> tools = result.getTools();
    context.assertEquals(5, tools.size(), "Should have 5 tools");
  }
}
