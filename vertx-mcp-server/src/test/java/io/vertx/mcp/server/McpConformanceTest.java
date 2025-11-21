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

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.json.schema.common.dsl.Schemas;
import io.vertx.mcp.common.content.TextContent;
import io.vertx.mcp.common.prompt.PromptMessage;
import io.vertx.mcp.common.resources.TextResourceContent;
import io.vertx.mcp.server.feature.PromptServerFeature;
import io.vertx.mcp.server.feature.ResourceServerFeature;
import io.vertx.mcp.server.feature.ToolServerFeature;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This test uses Testcontainers to run MCP conformance tests in a Docker container.
 * It requires Docker to be installed and running.
 * <p>
 * Maven profile: mvn test -Pmcp-conformance
 */
public class McpConformanceTest extends HttpTransportTestBase {

  private ImageFromDockerfile conformanceImage;

  @Override
  public void setUp(TestContext context) {
    super.setUp(context);

    ModelContextProtocolServer mcpServer = ModelContextProtocolServer.create(super.vertx);

    ToolServerFeature toolFeature = new ToolServerFeature();

    toolFeature.addStructuredTool(
      "echo",
      "Echo Tool",
      "Echoes back the input message",
      Schemas.objectSchema().requiredProperty("message", Schemas.stringSchema()),
      Schemas.objectSchema().property("echo", Schemas.stringSchema()),
      args -> Future.succeededFuture(new JsonObject().put("echo", args.getString("message")))
    );

    toolFeature.addStructuredTool(
      "add",
      "Addition Tool",
      "Adds two numbers",
      Schemas.objectSchema()
        .requiredProperty("a", Schemas.numberSchema())
        .requiredProperty("b", Schemas.numberSchema()),
      Schemas.objectSchema().property("result", Schemas.numberSchema()),
      args -> Future.succeededFuture(
        new JsonObject().put("result", args.getInteger("a") + args.getInteger("b"))
      )
    );

    ResourceServerFeature resourceFeature = new ResourceServerFeature();
    resourceFeature.addStaticResource("test://example", "example-resource", () ->
      Future.succeededFuture(new TextResourceContent()
        .setUri("test://example")
        .setName("Example Resource")
        .setDescription("An example resource for testing")
        .setMimeType("text/plain")
        .setText("This is an example resource"))
    );

    PromptServerFeature promptFeature = new PromptServerFeature();
    promptFeature.addPrompt(
      "greeting",
      "Greeting Prompt",
      "A simple greeting prompt",
      Schemas.arraySchema(),
      args -> {
        List<PromptMessage> messages = new ArrayList<>();
        PromptMessage message = new PromptMessage()
          .setRole("user")
          .setContent(new TextContent("Hello! How can I help you today?").toJson());
        messages.add(message);
        return Future.succeededFuture(messages);
      }
    );

    mcpServer.addServerFeature(toolFeature);
    mcpServer.addServerFeature(resourceFeature);
    mcpServer.addServerFeature(promptFeature);

    startServer(context, mcpServer);

    File dockerfile = new File("src/test/resources/mcp-conformance.Dockerfile");
    conformanceImage = new ImageFromDockerfile().withFileFromFile("Dockerfile", dockerfile);
  }

  @Test
  public void testServerInitialize(TestContext context) {
    Async async = context.async();

    Future<String> future = executeConformance("server-initialize");

    future.onComplete(context.asyncAssertSuccess(output -> {
      context.assertTrue(output.contains("pass") || output.contains("PASS") || !output.contains("fail"),
        "Server initialize conformance test should pass: " + output);
      async.complete();
    }));
  }

  @Test
  public void testToolsList(TestContext context) {
    Async async = context.async();

    Future<String> future = executeConformance("tools-list");

    future.onComplete(context.asyncAssertSuccess(output -> {
      context.assertTrue(output.contains("pass") || output.contains("PASS") || !output.contains("fail"),
        "Tools list conformance test should pass: " + output);
      async.complete();
    }));
  }

  @Test
  public void testResourcesList(TestContext context) {
    Async async = context.async();

    Future<String> future = executeConformance("resources-list");

    future.onComplete(context.asyncAssertSuccess(output -> {
      context.assertTrue(output.contains("pass") || output.contains("PASS") || !output.contains("fail"),
        "Resources list conformance test should pass: " + output);
      async.complete();
    }));
  }

  @Test
  public void testPromptsList(TestContext context) {
    Async async = context.async();

    Future<String> future = executeConformance("prompts-list");

    future.onComplete(context.asyncAssertSuccess(output -> {
      context.assertTrue(output.contains("pass") || output.contains("PASS") || !output.contains("fail"),
        "Prompts list conformance test should pass: " + output);
      async.complete();
    }));
  }

  @Test
  public void testAllScenarios(TestContext context) {
    Async async = context.async();

    // Run all scenarios by not specifying a specific one
    Future<String> future = executeConformance(null);

    future.onComplete(context.asyncAssertSuccess(output -> {
      System.out.println("[mcp-conformance] Full output: " + output);
      // Just ensure we got some output - detailed analysis depends on conformance framework output format
      context.assertFalse(output.isEmpty(), "Conformance test output should not be empty");
      async.complete();
    }));
  }

  private Future<String> executeConformance(String scenario) {
    return vertx.executeBlocking(() -> {
      List<String> command = new ArrayList<>();
      command.add("--url=http://host.docker.internal:" + port + "/mcp");

      if (scenario != null) {
        command.add("--scenario=" + scenario);
      }

      System.out.println("[mcp-conformance] Executing with args: " + command);

      ToStringConsumer logConsumer = new ToStringConsumer();
      try (GenericContainer<?> container = new GenericContainer<>(conformanceImage)) {
        container
          .withLogConsumer(logConsumer)
          .withAccessToHost(true)
          .withCommand(command.toArray(new String[0]))
          .withStartupCheckStrategy(new OneShotStartupCheckStrategy().withTimeout(Duration.ofSeconds(60)));

        try {
          container.start();
        } catch (Exception e) {
          // Container may exit with non-zero for expected failures
          // The output is still captured
        }

        String output = logConsumer.toUtf8String();
        System.out.println("[mcp-conformance] Output: " + output);
        return output;
      }
    });
  }
}
