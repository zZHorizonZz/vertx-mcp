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
import io.vertx.mcp.client.ModelContextProtocolClient;
import io.vertx.mcp.client.transport.http.StreamableHttpClientTransport;
import io.vertx.mcp.common.Implementation;
import io.vertx.mcp.common.capabilities.ClientCapabilities;
import io.vertx.mcp.common.notification.InitializedNotification;
import io.vertx.mcp.common.request.CallToolRequest;
import io.vertx.mcp.common.request.InitializeRequest;
import io.vertx.mcp.common.request.ListToolsRequest;
import io.vertx.mcp.common.result.InitializeResult;
import io.vertx.mcp.common.result.ListToolsResult;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * MCP conformance test client.
 * <p>
 * Executes MCP protocol scenarios using the vertx-mcp-client library. Called by the conformance framework to validate client behavior.
 * <p>
 * Usage: ConformanceClient <scenario> <server-url>
 */
public class ConformanceClient {

  private static final String CLIENT_NAME = "mcp-conformance-client";
  private static final String CLIENT_VERSION = "1.0.0";
  private static final int TIMEOUT_SECONDS = 60;

  public static void main(String[] args) {
    if (args.length < 2) {
      System.err.println("Usage: ConformanceClient <scenario> <server-url>");
      System.exit(1);
    }

    String scenario = args[0];
    String serverUrl = args[1];
    Vertx vertx = Vertx.vertx();
    CountDownLatch latch = new CountDownLatch(1);

    runScenario(vertx, scenario, serverUrl)
      .onComplete(ar -> {
        latch.countDown();
        int exitCode = ar.succeeded() ? 0 : 1;
        if (ar.failed()) {
          System.err.println("Scenario failed: " + ar.cause().getMessage());
          ar.cause().printStackTrace();
        }
        vertx.close().onComplete(v -> System.exit(exitCode));
      });

    try {
      if (!latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        System.err.println("Timeout");
        vertx.close();
        System.exit(1);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      vertx.close();
      System.exit(1);
    }
  }

  private static Future<Void> runScenario(Vertx vertx, String scenario, String serverUrl) {
    ClientOptions options = new ClientOptions()
      .setClientName(CLIENT_NAME)
      .setClientVersion(CLIENT_VERSION);

    StreamableHttpClientTransport transport = new StreamableHttpClientTransport(vertx, serverUrl, options);
    ModelContextProtocolClient client = ModelContextProtocolClient.create(vertx, transport, options);

    switch (scenario) {
      case "initialize":
        return initialize(client);
      case "tools_call":
        return initialize(client)
          .compose(v -> listTools(client))
          .compose(v -> callTool(client));
      default:
        return initialize(client);
    }
  }

  private static Future<Void> initialize(ModelContextProtocolClient client) {
    InitializeRequest request = new InitializeRequest()
      .setProtocolVersion(StreamableHttpClientTransport.DEFAULT_PROTOCOL_VERSION)
      .setCapabilities(new ClientCapabilities())
      .setClientInfo(new Implementation()
        .setName(CLIENT_NAME)
        .setVersion(CLIENT_VERSION));

    return client.sendRequest(request)
      .compose(result -> {
        InitializeResult initResult = (InitializeResult) result;
        System.out.println("Initialized with server: " + initResult.getServerInfo().getName());
        return client.sendNotification(new InitializedNotification(), null);
      });
  }

  private static Future<Void> listTools(ModelContextProtocolClient client) {
    return client.sendRequest(new ListToolsRequest())
      .compose(result -> {
        ListToolsResult listResult = (ListToolsResult) result;
        System.out.println("Found " + listResult.getTools().size() + " tools");
        return Future.succeededFuture();
      });
  }

  private static Future<Void> callTool(ModelContextProtocolClient client) {
    CallToolRequest request = new CallToolRequest()
      .setName("add_numbers")
      .setArguments(new JsonObject()
        .put("a", 5)
        .put("b", 7));

    return client.sendRequest(request)
      .compose(result -> {
        System.out.println("Tool result: " + result);
        return Future.succeededFuture();
      });
  }
}
