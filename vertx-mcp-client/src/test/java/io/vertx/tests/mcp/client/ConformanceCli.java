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
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
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
import io.vertx.mcp.common.rpc.JsonCodec;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * CLI tool for MCP conformance testing.
 * Usage: ConformanceCli <scenario> <server-url>
 */
public class ConformanceCli {

  public static void main(String[] args) {
    if (args.length < 2) {
      System.err.println("Usage: ConformanceCli <scenario> <server-url>");
      System.exit(1);
    }

    String scenario = args[0];
    String serverUrl = args[1];

    System.out.println("[cli] Starting conformance client for scenario: " + scenario);
    System.out.println("[cli] Server URL: " + serverUrl);

    Vertx vertx = Vertx.vertx();
    CountDownLatch latch = new CountDownLatch(1);

    try {
      runScenario(vertx, scenario, serverUrl)
        .onComplete(ar -> {
          if (ar.succeeded()) {
            System.out.println("[cli] Scenario completed successfully");
            System.exit(0);
          } else {
            System.err.println("[cli] Scenario failed: " + ar.cause().getMessage());
            ar.cause().printStackTrace();
            System.exit(1);
          }
          latch.countDown();
        });

      // Wait for completion
      latch.await(60, TimeUnit.SECONDS);
    } catch (Exception e) {
      System.err.println("[cli] Error: " + e.getMessage());
      e.printStackTrace();
      System.exit(1);
    } finally {
      vertx.close();
    }
  }

  private static Future<Void> runScenario(Vertx vertx, String scenario, String serverUrl) {
    ClientOptions options = new ClientOptions()
      .setClientName("mcp-client-conformance")
      .setClientVersion("1.0.0")
      .setStreamingEnabled(false);

    ClientTransport transport = new StreamableHttpClientTransport(vertx, serverUrl, options);
    ModelContextProtocolClient client = ModelContextProtocolClient.create(vertx, transport, options);

    System.out.println("[cli] Connecting to server...");

    return client.connect(new ClientCapabilities())
      .compose(session -> {
        System.out.println("[cli] Connected successfully");
        return executeScenario(scenario, session);
      })
      .compose(v -> {
        System.out.println("[cli] Scenario actions completed");
        return Future.succeededFuture();
      });
  }

  private static Future<Void> executeScenario(String scenario, ClientSession session) {
    switch (scenario) {
      case "initialize":
        // Connection already performs initialization
        return Future.succeededFuture();

      case "tools-call":
        return executeToolsCall(session);

      default:
        System.out.println("[cli] Unknown scenario: " + scenario + ", performing basic initialization");
        return Future.succeededFuture();
    }
  }

  private static Future<Void> executeToolsCall(ClientSession session) {
    System.out.println("[cli] Executing tools-call scenario");

    // List tools
    return session.sendRequest(new ListToolsRequest())
      .compose(result -> {
        ListToolsResult listResult = (ListToolsResult) JsonCodec.decodeResult(
          ListToolsRequest.METHOD,
          (JsonObject) result.getResult()
        );
        System.out.println("[cli] Found " + listResult.getTools().size() + " tools");

        // Call the add_numbers tool
        JsonObject callParams = new JsonObject()
          .put("name", "add_numbers")
          .put("arguments", new JsonObject()
            .put("a", 5)
            .put("b", 7));

        return session.sendRequest(new CallToolRequest(callParams));
      })
      .compose(result -> {
        CallToolResult callResult = (CallToolResult) JsonCodec.decodeResult(
          CallToolRequest.METHOD,
          (JsonObject) result.getResult()
        );
        System.out.println("[cli] Tool call result: " + callResult.getContent());
        return Future.succeededFuture();
      });
  }
}
