/*
 * Copyright (c) 2011-2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.mcp.server;

import dev.jbang.jash.Jash;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.json.schema.common.dsl.Schemas;
import io.vertx.mcp.common.content.TextContent;
import io.vertx.mcp.common.prompt.PromptMessage;
import io.vertx.mcp.common.resources.TextResourceContent;
import io.vertx.mcp.server.impl.PromptServerFeature;
import io.vertx.mcp.server.impl.ResourceServerFeature;
import io.vertx.mcp.server.impl.ToolServerFeature;
import org.junit.Test;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static dev.jbang.jash.Jash.$;

/**
 * This test requires Node.js and npx to be installed on the system.
 * It is excluded from the default test run and can be enabled by activating the "mcp-inspector" profile.
 * <p>
 * Maven profile: mvn test -Pmcp-inspector
 */
public class McpInspectorTest extends HttpTransportTestBase {

  @Override
  public void setUp(TestContext context) {
    super.setUp(context);

    // Create MCP server with tools, resources, and prompts
    ModelContextProtocolServer mcpServer = ModelContextProtocolServer.create();

    // Add tools
    ToolServerFeature toolFeature = new ToolServerFeature();
    toolFeature.addStructuredTool(
      "echo",
      "Echo Tool",
      "Echoes back the input message",
      StructuredToolHandler.create(
        Schemas.objectSchema()
          .requiredProperty("message", Schemas.stringSchema()),
        Schemas.objectSchema()
          .property("echo", Schemas.stringSchema()),
        args -> Future.succeededFuture(new JsonObject().put("echo", args.getString("message")))
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
        args -> Future.succeededFuture(
          new JsonObject().put("result", args.getInteger("a") + args.getInteger("b"))
        )
      )
    );

    // Add resources
    ResourceServerFeature resourceFeature = new ResourceServerFeature();
    resourceFeature.addStaticResource("example-resource", () ->
      Future.succeededFuture(new TextResourceContent()
        .setUri("test://example")
        .setName("Example Resource")
        .setDescription("An example resource for testing")
        .setMimeType("text/plain")
        .setText("This is an example resource"))
    );

    // Add prompts
    PromptServerFeature promptFeature = new PromptServerFeature();
    promptFeature.addPrompt(
      "greeting",
      "Greeting Prompt",
      "A simple greeting prompt",
      PromptHandler.create(
        Schemas.arraySchema(), // Array schema for prompt arguments
        args -> {
          List<PromptMessage> messages = new ArrayList<>();
          PromptMessage message = new PromptMessage()
            .setRole("user")
            .setContent(new TextContent("Hello! How can I help you today?").toJson());
          messages.add(message);
          return Future.succeededFuture(messages);
        }
      )
    );

    mcpServer.serverFeatures(toolFeature);
    mcpServer.serverFeatures(resourceFeature);
    mcpServer.serverFeatures(promptFeature);

    startServer(context, mcpServer);
  }

  @Test
  public void testListTools(TestContext context) {
    Async async = context.async();

    Future<String> future = executeInspector("--method tools/list");

    future.onComplete(context.asyncAssertSuccess(output -> {
      context.assertTrue(output.contains("\"name\": \"echo\""),
        "Output should contain echo tool: " + output);
      context.assertTrue(output.contains("\"name\": \"add\""),
        "Output should contain add tool: " + output);
      async.complete();
    }));
  }

  @Test
  public void testCallEchoTool(TestContext context) {
    Async async = context.async();

    Future<String> future = executeInspector(
      "--method tools/call --tool-name echo --tool-arg message=\"Hello MCP\""
    );

    future.onComplete(context.asyncAssertSuccess(output -> {
      context.assertTrue(output.contains("Hello MCP"),
        "Output should contain the echoed message: " + output);
      async.complete();
    }));
  }

  @Test
  public void testCallAddTool(TestContext context) {
    Async async = context.async();

    Future<String> future = executeInspector(
      "--method tools/call --tool-name add --tool-arg a=5 --tool-arg b=3"
    );

    future.onComplete(context.asyncAssertSuccess(output -> {
      context.assertTrue(output.contains("\"result\": 8") || output.contains("\"result\":8"),
        "Output should contain result of 8: " + output);
      async.complete();
    }));
  }

  @Test
  public void testListResources(TestContext context) {
    Async async = context.async();

    Future<String> future = executeInspector("--method resources/list");

    future.onComplete(context.asyncAssertSuccess(output -> {
      context.assertTrue(output.contains("test://example"),
        "Output should contain the example resource URI: " + output);
      async.complete();
    }));
  }

  @Test
  public void testListPrompts(TestContext context) {
    Async async = context.async();

    Future<String> future = executeInspector("--method prompts/list");

    future.onComplete(context.asyncAssertSuccess(output -> {
      context.assertTrue(output.contains("\"name\": \"greeting\"") || output.contains("\"name\":\"greeting\""),
        "Output should contain greeting prompt: " + output);
      async.complete();
    }));
  }

  @Test
  public void testCallToolWithJsonArguments(TestContext context) {
    Async async = context.async();

    Future<String> future = executeInspector(
      "--method tools/call --tool-name echo --tool-arg 'message={\"nested\":\"value\"}'"
    );

    future.onComplete(context.asyncAssertSuccess(output -> {
      // The output should contain the result
      context.assertTrue(output.length() > 0,
        "Output should not be empty: " + output);
      async.complete();
    }));
  }

  @Test
  public void testInvalidToolCall(TestContext context) {
    Async async = context.async();

    Future<String> future = executeInspector(
      "--method tools/call --tool-name nonexistent --tool-arg arg=value"
    );

    future.onComplete(context.asyncAssertSuccess(output -> {
      // Inspector should handle the error gracefully
      // The server will return an error for unknown tool
      context.assertTrue(output.length() > 0,
        "Output should not be empty: " + output);
      async.complete();
    }));
  }

  /**
   * Execute MCP Inspector CLI command
   */
  private Future<String> executeInspector(String args) {
    return vertx.executeBlocking(() -> {
      try {
        String serverUrl = "http://localhost:" + port + "/mcp";
        String command = "npx @modelcontextprotocol/inspector --cli " + serverUrl +
                        " --transport http " + args;

        System.out.println("[mcp-inspector] Executing: " + command);

        try (Jash process = $(command)
          .withAllowedExitCodes(0, 1) // Allow both success and error codes
          .withTimeout(Duration.of(30, ChronoUnit.SECONDS))) {

          String output = process.stream().collect(Collectors.joining("\n"));
          System.out.println("[mcp-inspector] Output: " + output);
          return output;
        }
      } catch (Exception e) {
        throw new RuntimeException("Failed to execute MCP Inspector command", e);
      }
    });
  }
}
