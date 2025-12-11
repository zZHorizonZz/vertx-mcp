package io.vertx.mcp.demo;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.client.ClientOptions;
import io.vertx.mcp.client.ClientSession;
import io.vertx.mcp.client.ClientTransport;
import io.vertx.mcp.client.ModelContextProtocolClient;
import io.vertx.mcp.client.impl.ModelContextProtocolClientImpl;
import io.vertx.mcp.client.transport.http.StreamableHttpClientTransport;
import io.vertx.mcp.common.capabilities.ClientCapabilities;
import io.vertx.mcp.common.notification.ProgressNotification;
import io.vertx.mcp.common.prompt.PromptMessage;
import io.vertx.mcp.common.request.*;
import io.vertx.mcp.common.result.*;
import io.vertx.mcp.common.tool.Tool;

/**
 * A comprehensive MCP client demo showcasing the client API features.
 *
 * <p>This demo connects to the MCPServerDemo and demonstrates:
 * <ul>
 *   <li><b>Connection</b> - Connecting to an MCP server via HTTP</li>
 *   <li><b>Tools</b> - Listing and calling tools</li>
 *   <li><b>Bulk Import</b> - Calling tools with progress and logging notifications</li>
 *   <li><b>Resources</b> - Listing and reading resources</li>
 *   <li><b>Prompts</b> - Getting prompts</li>
 * </ul>
 *
 * <p>Prerequisites: Start MCPServerDemo before running this demo.
 */
public class MCPClientDemo {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();

    // Configure client options
    ClientOptions clientOptions = new ClientOptions()
      .setClientName("mcp-client-demo")
      .setClientVersion("1.0.0")
      .setStreamingEnabled(true);

    // Server URL
    String serverUrl = System.getProperty("server.url", "http://localhost:8080/mcp");

    // Create MCP client
    ClientTransport transport = new StreamableHttpClientTransport(vertx, serverUrl, clientOptions);
    ModelContextProtocolClient client = new ModelContextProtocolClientImpl(vertx, transport, clientOptions);

    client.addNotificationHandler("notifications/progress", notification -> {
      ProgressNotification progress = (ProgressNotification) notification;
      System.out.println("  Progress: " + (progress.getProgress() + 1) + "/5");
    });

    // Register notification handlers to receive progress and logging updates
    //client.addNotificationHandler(ProgressNotificationHandler.defaultHandler());
    //client.addNotificationHandler(LoggingNotificationHandler.defaultHandler());

    System.out.println("=".repeat(60));
    System.out.println("MCP Client Demo");
    System.out.println("=".repeat(60));
    System.out.println("Connecting to: " + serverUrl);
    System.out.println();

    try {
      // Connect to server
      ClientCapabilities capabilities = new ClientCapabilities();
      ClientSession session = client.subscribe(capabilities).await();

      System.out.println("  Connected to server");
      System.out.println("  Session ID: " + session.id());
      System.out.println();

      // Run demos sequentially
      demonstrateListTools(client, session).await();
      demonstrateCallTool(client, session).await();
      demonstrateBulkImport(client, session).await();
      demonstrateListResources(client, session).await();
      demonstrateReadResource(client, session).await();
      demonstrateGetPrompt(client, session).await();

      System.out.println();
      System.out.println("=".repeat(60));
      System.out.println("Demo completed successfully!");
      System.out.println("=".repeat(60));
    } catch (Exception err) {
      System.err.println();
      System.err.println("=".repeat(60));
      System.err.println("Demo failed: " + err.getMessage());
      System.err.println("=".repeat(60));
      err.printStackTrace();
    } finally {
      vertx.close();
    }
  }

  /**
   * Demonstrates listing available tools on the server.
   */
  private static Future<Void> demonstrateListTools(ModelContextProtocolClient client, ClientSession session) {
    System.out.println("-".repeat(60));
    System.out.println("1. Listing Tools");
    System.out.println("-".repeat(60));

    // Create list tools sendRequest
    return client.sendRequest(new ListToolsRequest())
      .expecting(result -> result instanceof ListToolsResult)
      .compose(result -> {
        System.out.println("  Received tools list:");
        ListToolsResult listToolsResult = (ListToolsResult) result;

        for (Tool tool : listToolsResult.getTools()) {
          System.out.println("  - " + tool.getName() + ": " + tool.getDescription());
        }

        System.out.println("  Total: " + listToolsResult.getTools().size() + " tools");
        System.out.println();
        return Future.succeededFuture();
      })
      .onFailure(err -> {
        System.err.println("  Failed to list tools: " + err.getMessage());
        err.printStackTrace();
      }).mapEmpty();
  }

  /**
   * Demonstrates calling a tool on the server.
   */
  private static Future<Void> demonstrateCallTool(ModelContextProtocolClient client, ClientSession session) {
    System.out.println("-".repeat(60));
    System.out.println("2. Calling Tool: list_tasks");
    System.out.println("-".repeat(60));

    // Create call tool sendRequest
    CallToolRequest callToolRequest = new CallToolRequest()
      .setName("list_tasks")
      .setArguments(new JsonObject().put("status", "in_progress"));

    return client.sendRequest(callToolRequest)
      .expecting(result -> result instanceof CallToolResult)
      .compose(result -> {
        System.out.println("  Tool executed successfully");
        CallToolResult callToolResult = (CallToolResult) result;

        JsonArray content = callToolResult.getContent();
        if (content != null) {
          for (int i = 0; i < content.size(); i++) {
            JsonObject item = content.getJsonObject(i);
            if ("text".equals(item.getString("type"))) {
              System.out.println("  " + item.getString("text"));
            }
          }
        }
        System.out.println();
        return Future.succeededFuture();
      })
      .onFailure(err -> {
        System.err.println("  Failed to call tool: " + err.getMessage());
        err.printStackTrace();
      }).mapEmpty();
  }

  /**
   * Demonstrates listing available resources on the server.
   */
  private static Future<Void> demonstrateListResources(ModelContextProtocolClient client, ClientSession session) {
    System.out.println("-".repeat(60));
    System.out.println("4. Listing Resources");
    System.out.println("-".repeat(60));

    // Create list resources sendRequest
    return client.sendRequest(new ListResourcesRequest())
      .expecting(result -> result instanceof ListResourcesResult)
      .compose(result -> {
        System.out.println("  Received resources list:");
        ListResourcesResult listResourcesResult = (ListResourcesResult) result;

        JsonArray resources = listResourcesResult.getResources();
        if (resources != null) {
          for (int i = 0; i < resources.size(); i++) {
            JsonObject resource = resources.getJsonObject(i);
            System.out.println("  - " + resource.getString("uri") + ": " +
              resource.getString("name"));
            String desc = resource.getString("description");
            if (desc != null && !desc.isEmpty()) {
              System.out.println("    " + desc);
            }
          }
          System.out.println("  Total: " + resources.size() + " resources");
        }
        System.out.println();
        return Future.succeededFuture();
      })
      .onFailure(err -> {
        System.err.println("  Failed to list resources: " + err.getMessage());
        err.printStackTrace();
      }).mapEmpty();
  }

  /**
   * Demonstrates reading a resource from the server.
   */
  private static Future<Void> demonstrateReadResource(ModelContextProtocolClient client, ClientSession session) {
    System.out.println("-".repeat(60));
    System.out.println("5. Reading Resource: tasks://all");
    System.out.println("-".repeat(60));

    // Create read resource sendRequest
    ReadResourceRequest readResourceRequest = new ReadResourceRequest()
      .setUri("tasks://all");

    return client.sendRequest(readResourceRequest)
      .expecting(result -> result instanceof ReadResourceResult)
      .compose(result -> {
        System.out.println("  Resource read successfully");
        ReadResourceResult readResourceResult = (ReadResourceResult) result;

        JsonArray contents = readResourceResult.getContents();
        if (contents != null && !contents.isEmpty()) {
          JsonObject content = contents.getJsonObject(0);
          String text = content.getString("text");
          if (text != null) {
            // Parse and display the JSON content
            try {
              JsonArray tasks = new JsonArray(text);
              System.out.println("  Total tasks: " + tasks.size());
              System.out.println("  Sample tasks:");
              for (int i = 0; i < Math.min(3, tasks.size()); i++) {
                JsonObject task = tasks.getJsonObject(i);
                System.out.println("    • " + task.getString("title") +
                  " [" + task.getString("id") + "]");
              }
            } catch (Exception e) {
              System.out.println("  Content: " + text.substring(0, Math.min(100, text.length())) + "...");
            }
          }
        }
        System.out.println();
        return Future.succeededFuture();
      })
      .onFailure(err -> {
        System.err.println("  Failed to read resource: " + err.getMessage());
        err.printStackTrace();
      }).mapEmpty();
  }

  /**
   * Demonstrates calling the bulk import tool with progress notifications. This showcases how the client can receive real-time progress updates during long-running operations.
   */
  private static Future<Void> demonstrateBulkImport(ModelContextProtocolClient client, ClientSession session) {
    System.out.println("-".repeat(60));
    System.out.println("3. Bulk Import with Progress Notifications");
    System.out.println("-".repeat(60));

    // Create sample tasks to import
    JsonArray tasks = new JsonArray()
      .add(new JsonObject()
        .put("title", "Implement JWT authentication")
        .put("description", "Add JWT token-based authentication to the API")
        .put("priority", "high")
        .put("assignee", "alice@example.com"))
      .add(new JsonObject()
        .put("title", "Add rate limiting")
        .put("description", "Implement rate limiting middleware")
        .put("priority", "medium")
        .put("assignee", "bob@example.com"))
      .add(new JsonObject()
        .put("title", "Write API documentation")
        .put("description", "Document all REST endpoints")
        .put("priority", "low")
        .put("assignee", "carol@example.com"))
      .add(new JsonObject()
        .put("title", "Setup CI/CD pipeline")
        .put("description", "Configure GitHub Actions for automated testing and deployment")
        .put("priority", "critical")
        .put("assignee", "dave@example.com"))
      .add(new JsonObject()
        .put("title", "Optimize database queries")
        .put("description", "Add indexes and optimize slow queries")
        .put("priority", "high")
        .put("assignee", "eve@example.com"));

    System.out.println("  Importing " + tasks.size() + " tasks...");
    System.out.println("  Watch for progress notifications below:");
    System.out.println();

    // Create call tool sendRequest with progress token
    CallToolRequest callToolRequest = new CallToolRequest()
      .setName("bulk_import")
      .setArguments(new JsonObject().put("tasks", tasks));
    //.setProgressToken("bulk-import-" + System.currentTimeMillis());

    callToolRequest.setMeta(new JsonObject().put("progressToken", true));

    return client.sendRequest(callToolRequest)
      .expecting(result -> result instanceof CallToolResult)
      .compose(result -> {
        System.out.println();
        System.out.println("  Bulk import completed successfully");
        CallToolResult callToolResult = (CallToolResult) result;

        JsonArray content = callToolResult.getContent();
        if (content != null) {
          for (int i = 0; i < content.size(); i++) {
            JsonObject item = content.getJsonObject(i);
            if ("text".equals(item.getString("type"))) {
              System.out.println("  Result: " + item.getString("text"));
            }
          }
        }
        System.out.println();
        return Future.succeededFuture();
      })
      .onFailure(err -> {
        System.err.println("  Failed to bulk import: " + err.getMessage());
        err.printStackTrace();
      }).mapEmpty();
  }

  /**
   * Demonstrates getting a prompt from the server.
   */
  private static Future<Void> demonstrateGetPrompt(ModelContextProtocolClient client, ClientSession session) {
    System.out.println("-".repeat(60));
    System.out.println("6. Getting Prompt: daily_standup");
    System.out.println("-".repeat(60));

    // Create get prompt sendRequest
    GetPromptRequest getPromptRequest = new GetPromptRequest()
      .setName("daily_standup")
      .setArguments(new JsonObject().put("assignee", "julien@vertx.io"));

    return client.sendRequest(getPromptRequest)
      .expecting(result -> result instanceof GetPromptResult)
      .compose(result -> {
        System.out.println("  Prompt retrieved successfully");
        GetPromptResult getPromptResult = (GetPromptResult) result;

        if (getPromptResult.getMessages() != null) {
          System.out.println("  Messages: " + getPromptResult.getMessages().size());
          if (!getPromptResult.getMessages().isEmpty()) {
            PromptMessage message = getPromptResult.getMessages().get(0);
            System.out.println("  Role: " + message.getRole());
            JsonObject content = message.getContent();
            if (content != null) {
              String text = content.getString("text");
              if (text != null) {
                // Show first 200 chars
                System.out.println("  Content preview:");
                String preview = text.substring(0, Math.min(200, text.length()));
                System.out.println("    " + preview.replace("\n", "\n    "));
                if (text.length() > 200) {
                  System.out.println("    ...");
                }
              }
            }
          }
        }
        System.out.println();
        return Future.succeededFuture();
      })
      .onFailure(err -> {
        System.err.println("  Failed to get prompt: " + err.getMessage());
        err.printStackTrace();
      }).mapEmpty();
  }
}
