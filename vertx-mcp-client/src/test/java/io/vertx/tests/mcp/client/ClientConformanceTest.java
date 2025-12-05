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
package io.vertx.tests.mcp.client;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.mcp.client.ClientOptions;
import io.vertx.mcp.client.ClientSession;
import io.vertx.mcp.client.ClientTransport;
import io.vertx.mcp.client.ModelContextProtocolClient;
import io.vertx.mcp.client.transport.http.StreamableHttpClientTransport;
import io.vertx.mcp.common.capabilities.ClientCapabilities;
import io.vertx.mcp.common.request.CallToolRequest;
import io.vertx.mcp.common.request.ListToolsRequest;
import io.vertx.mcp.common.result.CallToolResult;
import io.vertx.mcp.common.result.ListToolsResult;
import io.vertx.tests.mcp.common.TestContainerTestBase;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * Client conformance tests using Testcontainers to run MCP scenario servers.
 * Each test runs a scenario server in a container and uses the Vert.x MCP client
 * to connect and perform operations. The scenario validates client behavior.
 * <p>
 * Maven profile: mvn test -Pmcp-client-conformance
 */
public class ClientConformanceTest extends TestContainerTestBase {

  private ImageFromDockerfile conformanceImage;

  @Override
  public void setUp(TestContext context) {
    super.setUp(context);

    File dockerfile = new File("src/test/resources/conformance.Dockerfile");
    conformanceImage = new ImageFromDockerfile().withFileFromFile("Dockerfile", dockerfile);
  }

  @Test
  public void testInitialize(TestContext context) throws Throwable {
    runScenario(context, "initialize", session -> {
      // Just connecting triggers initialize
      return Future.succeededFuture();
    });
  }

  @Test
  public void testToolsCall(TestContext context) throws Throwable {
    runScenario(context, "tools-call", session -> {
      // List tools
      return session.sendRequest(new ListToolsRequest())
        .expecting(r -> r instanceof ListToolsResult)
        .compose(result -> {
          ListToolsResult listResult = (ListToolsResult) result;
          System.out.println("[client] Found " + listResult.getTools().size() + " tools");

          // Call the add_numbers tool
          JsonObject callParams = new JsonObject()
            .put("name", "add_numbers")
            .put("arguments", new JsonObject()
              .put("a", 5)
              .put("b", 7));

          return session.sendRequest(new CallToolRequest(callParams))
            .expecting(r -> r instanceof CallToolResult);
        })
        .compose(result -> {
          CallToolResult callResult = (CallToolResult) result;
          System.out.println("[client] Tool call completed: " + callResult.getContent());
          return Future.succeededFuture();
        });
    });
  }

  private void runScenario(TestContext context, String scenario, ClientAction action) throws Throwable {
    System.out.println("[test] Running scenario: " + scenario);

    ToStringConsumer logConsumer = new ToStringConsumer();

    try (GenericContainer<?> container = new GenericContainer<>(conformanceImage)) {
      container
        .withEnv("SCENARIO", scenario)
        .withExposedPorts(3000)
        .withLogConsumer(logConsumer);

      container.start();

      int port = container.getMappedPort(3000);
      String serverUrl = "http://localhost:" + port;

      System.out.println("[test] Scenario server running at: " + serverUrl);

      // Wait a bit for scenario server to be ready
      Thread.sleep(2000);

      // Create and connect client
      ClientOptions options = new ClientOptions()
        .setClientName("mcp-client-conformance-test")
        .setClientVersion("1.0.0")
        .setStreamingEnabled(false);

      ClientTransport transport = new StreamableHttpClientTransport(vertx, serverUrl, options);
      ModelContextProtocolClient client = ModelContextProtocolClient.create(vertx, transport, options);

      System.out.println("[test] Connecting client...");
      ClientSession session = client.connect(new ClientCapabilities())
        .await(10, TimeUnit.SECONDS);

      System.out.println("[test] Client connected, executing scenario actions...");

      // Execute scenario-specific actions
      action.execute(session).await(30, TimeUnit.SECONDS);

      System.out.println("[test] Scenario actions completed");

      // Wait a bit for scenario to finalize checks
      Thread.sleep(1000);

      // Stop container and get output
      container.stop();

      String output = logConsumer.toUtf8String();
      System.out.println("[test] Scenario output:\n" + output);

      // Parse and validate output
      validateOutput(context, scenario, output);

      System.out.println("[test] Scenario completed: " + scenario);
    }
  }

  private void validateOutput(TestContext context, String scenario, String output) {
    System.out.println("\n[" + scenario + "] Validating conformance output:");
    System.out.println("=".repeat(60));

    // Count passed and failed from output
    int passedCount = 0;
    int failedCount = 0;

    // Look for check results in output
    // The conformance framework outputs results - parse them
    String[] lines = output.split("\n");
    for (String line : lines) {
      if (line.contains("SUCCESS") || line.contains("✓") || line.contains("PASS")) {
        passedCount++;
        System.out.println("[SUCCESS] " + line);
      } else if (line.contains("FAILURE") || line.contains("✗") || line.contains("FAIL")) {
        failedCount++;
        System.err.println("[FAILURE] " + line);
      }
    }

    System.out.println("=".repeat(60));
    System.out.println("Results: " + passedCount + " passed, " + failedCount + " failed\n");

    if (failedCount > 0) {
      context.fail("Scenario '" + scenario + "' had " + failedCount + " failures. See output above.");
    }

    if (passedCount == 0 && failedCount == 0) {
      System.out.println("Warning: Could not parse conformance results from output");
    }
  }

  @FunctionalInterface
  private interface ClientAction {
    Future<Void> execute(ClientSession session);
  }
}
