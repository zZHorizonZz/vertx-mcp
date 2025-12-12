package examples;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.client.*;
import io.vertx.mcp.client.feature.ElicitationClientFeature;
import io.vertx.mcp.client.feature.RootsClientFeature;
import io.vertx.mcp.client.feature.SamplingClientFeature;
import io.vertx.mcp.client.impl.ClientFeatureBase;
import io.vertx.mcp.client.transport.http.StreamableHttpClientTransport;
import io.vertx.mcp.common.capabilities.ClientCapabilities;
import io.vertx.mcp.common.capabilities.ServerCapabilities;
import io.vertx.mcp.common.content.TextContent;
import io.vertx.mcp.common.notification.LoggingMessageNotification;
import io.vertx.mcp.common.notification.ProgressNotification;
import io.vertx.mcp.common.request.*;
import io.vertx.mcp.common.result.*;
import io.vertx.mcp.common.root.Root;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.common.sampling.SamplingMessage;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class MCPClientExamples {

  public void createClient(Vertx vertx) {
    // Create HTTP transport
    String serverUrl = "http://localhost:8080/mcp";
    ClientTransport transport = new StreamableHttpClientTransport(vertx, serverUrl,
      new ClientOptions());

    // Create the MCP client
    ModelContextProtocolClient client = ModelContextProtocolClient.create(vertx,
      transport);
  }

  public void createClientWithOptions(Vertx vertx) {
    // Configure client options
    ClientOptions options = new ClientOptions()
      .setClientName("my-mcp-client")
      .setClientVersion("1.0.0")
      .setStreamingEnabled(true);

    // Create transport and client
    String serverUrl = "http://localhost:8080/mcp";
    ClientTransport transport = new StreamableHttpClientTransport(vertx, serverUrl,
      options);
    ModelContextProtocolClient client = ModelContextProtocolClient.create(vertx,
      transport, options);
  }

  public void clientOptions() {
    ClientOptions options = new ClientOptions()
      .setClientName("my-client")              // Client name sent to server
      .setClientVersion("1.0.0")               // Client version sent to server
      .setSessionsEnabled(true)                // Enable session management
      .setStreamingEnabled(
        true)               // Enable SSE streaming (requires sessions)
      .setNotificationsEnabled(true)           // Enable server notifications
      .setLoggingEnabled(
        true)                 // Enable logging notifications (requires sessions)
      .setRequestTimeoutMs(30000)              // Request timeout (30 seconds)
      .setConnectTimeoutMs(10000);             // Connection timeout (10 seconds)
  }

  public void httpTransport(Vertx vertx) {
    ClientOptions clientOptions = new ClientOptions();
    String serverUrl = "http://localhost:8080/mcp";

    // Create HTTP transport
    ClientTransport transport = new StreamableHttpClientTransport(vertx, serverUrl,
      clientOptions);
  }

  public void httpTransportWithOptions(Vertx vertx) {
    ClientOptions clientOptions = new ClientOptions();
    String serverUrl = "http://localhost:8080/mcp";

    // Configure HTTP client options
    HttpClientOptions httpOptions = new HttpClientOptions()
      .setConnectTimeout(5000)
      .setIdleTimeout(60)
      .setKeepAlive(true);

    // Create HTTP transport with custom HTTP options
    ClientTransport transport = new StreamableHttpClientTransport(
      vertx, serverUrl, clientOptions, httpOptions);
  }

  public void subscribe(Vertx vertx, ModelContextProtocolClient client) {
    // Define client capabilities
    ClientCapabilities capabilities = new ClientCapabilities();

    // Connect to the server
    client.subscribe(capabilities)
      .onSuccess(session -> {
        System.out.println("Connected with session ID: " + session.id());
      })
      .onFailure(err -> {
        System.err.println("Failed to connect: " + err.getMessage());
      });
  }

  public void sessionCapabilities(ClientSession session) {
    // Get server capabilities
    ServerCapabilities serverCaps = session.serverCapabilities();

    // Check if the server supports specific features
    if (serverCaps.getTools() != null) {
      System.out.println("Server supports tools");
    }
    if (serverCaps.getResources() != null) {
      System.out.println("Server supports resources");
      if (Boolean.TRUE.equals(serverCaps.getResources().getSubscribe())) {
        System.out.println("Server supports resource subscriptions");
      }
    }
    if (serverCaps.getPrompts() != null) {
      System.out.println("Server supports prompts");
    }
  }

  public void callTool(ModelContextProtocolClient client) {
    // List available tools
    client.sendRequest(new ListToolsRequest())
      .onSuccess(result -> {
        ListToolsResult toolsResult = (ListToolsResult) result;
        toolsResult.getTools().forEach(tool -> {
          System.out.println(
            "Tool: " + tool.getName() + " - " + tool.getDescription());
        });
      });

    // Call a tool with arguments
    CallToolRequest callRequest = new CallToolRequest()
      .setName("greet")
      .setArguments(new JsonObject().put("name", "World"));

    client.sendRequest(callRequest)
      .onSuccess(result -> {
        CallToolResult callResult = (CallToolResult) result;
        System.out.println("Tool result: " + callResult.getContent());
      });
  }

  public void readResource(ModelContextProtocolClient client) {
    // List available resources
    client.sendRequest(new ListResourcesRequest())
      .onSuccess(result -> {
        ListResourcesResult resourcesResult = (ListResourcesResult) result;
        resourcesResult.getResources().forEach(resource -> {
          System.out.println("Resource: " + resource);
        });
      });

    // Read a specific resource
    ReadResourceRequest readRequest = new ReadResourceRequest()
      .setUri("config://app/settings");

    client.sendRequest(readRequest)
      .onSuccess(result -> {
        ReadResourceResult readResult = (ReadResourceResult) result;
        readResult.getContents().forEach(content -> {
          System.out.println("Content: " + content);
        });
      });
  }

  public void getPrompt(ModelContextProtocolClient client) {
    // List available prompts
    client.sendRequest(new ListPromptsRequest())
      .onSuccess(result -> {
        ListPromptsResult promptsResult = (ListPromptsResult) result;
        promptsResult.getPrompts().forEach(prompt -> {
          System.out.println(
            "Prompt: " + prompt.getName() + " - " + prompt.getDescription());
        });
      });

    // Get a prompt with arguments
    GetPromptRequest promptRequest = new GetPromptRequest()
      .setName("code-review")
      .setArguments(new JsonObject()
        .put("code", "function hello() { return 'world'; }")
        .put("language", "javascript"));

    client.sendRequest(promptRequest)
      .onSuccess(result -> {
        GetPromptResult promptResult = (GetPromptResult) result;
        promptResult.getMessages().forEach(message -> {
          System.out.println("Role: " + message.getRole());
          System.out.println("Content: " + message.getContent());
        });
      });
  }

  public void notificationHandlers(ModelContextProtocolClient client) {
    // Handle progress notifications
    client.addNotificationHandler("notifications/progress", notification -> {
      ProgressNotification progress = (ProgressNotification) notification;
      System.out.println(
        "Progress: " + progress.getProgress() + "/" + progress.getTotal());
    });

    // Handle logging notifications
    client.addNotificationHandler("notifications/message", notification -> {
      LoggingMessageNotification log = (LoggingMessageNotification) notification;
      System.out.println("[" + log.getLevel() + "] " + log.getData());
    });

    // Handle resource updates
    client.addNotificationHandler("notifications/resources/updated",
      notification -> {
        System.out.println("Resource updated: " + notification.toJson());
      });
  }

  public void rootsFeature(ModelContextProtocolClient client) {
    // Create and configure roots feature
    RootsClientFeature rootsFeature = new RootsClientFeature()
      .addRoot(new Root()
        .setName("workspace")
        .setUri("file:///home/user/workspace"))
      .addRoot(new Root()
        .setName("projects")
        .setUri("file:///home/user/projects"));

    // Register the feature with the client
    client.addClientFeature(rootsFeature);
  }

  public void manageRoots(RootsClientFeature rootsFeature) {
    // Add a new root
    rootsFeature.addRoot(new Root()
      .setName("documents")
      .setUri("file:///home/user/documents"));

    // Remove a root by name
    rootsFeature.removeRoot("documents");

    // Get all registered roots
    rootsFeature.roots().forEach((name, root) -> {
      System.out.println("Root: " + name + " -> " + root.getUri());
    });
  }

  public void samplingFeature(ModelContextProtocolClient client) {
    // Create sampling feature with handler
    SamplingClientFeature samplingFeature = new SamplingClientFeature()
      .setSamplingHandler(request -> {
        // Handle server's request for LLM completion
        List<SamplingMessage> messages = request.getMessages();
        String model = request.getModelPreferences() != null ?
          request.getModelPreferences().toString() : "default";

        // Generate response (integrate with your LLM provider here)
        CreateMessageResult result = new CreateMessageResult()
          .setRole("assistant")
          .setContent(new TextContent("Response from LLM").toJson())
          .setModel(model);

        return Future.succeededFuture(result);
      });

    // Register the feature
    client.addClientFeature(samplingFeature);
  }

  public void elicitationFeature(ModelContextProtocolClient client) {
    // Create elicitation feature with handler
    ElicitationClientFeature elicitationFeature = new ElicitationClientFeature()
      .setElicitationHandler(request -> {
        // Handle server's request for user input
        String message = request.getMessage();
        JsonObject schema = request.getRequestedSchema();

        // Collect user input (integrate with your UI here)
        ElicitResult result = new ElicitResult()
          .setAction("accept")
          .setContent(new JsonObject().put("userInput", "User provided value"));

        return Future.succeededFuture(result);
      });

    // Register the feature
    client.addClientFeature(elicitationFeature);
  }

  public void customFeature(ModelContextProtocolClient client) {
    // Create a custom feature
    ClientFeatureBase customFeature = new ClientFeatureBase() {
      @Override
      public Map<String, Function<JsonRequest, Future<JsonObject>>> getHandlers() {
        return Map.of(
          "custom/operation", this::handleCustomOperation
        );
      }

      private Future<JsonObject> handleCustomOperation(JsonRequest request) {
        // Process the request
        JsonObject params = request.getNamedParams();

        // Return response
        JsonObject result = new JsonObject().put("status", "success");
        return Future.succeededFuture(
          JsonResponse.success(request, result).toJson());
      }
    };

    // Register the custom feature
    client.addClientFeature(customFeature);
  }

  public void errorHandling(ModelContextProtocolClient client) {
    CallToolRequest request = new CallToolRequest()
      .setName("unknown-tool")
      .setArguments(new JsonObject());

    client.sendRequest(request)
      .onSuccess(result -> {
        System.out.println("Success: " + result);
      })
      .onFailure(err -> {
        if (err instanceof ClientRequestException) {
          ClientRequestException reqErr = (ClientRequestException) err;
          System.err.println("Error code: " + reqErr.getCode());
          System.err.println("Error message: " + reqErr.getMessage());
          System.err.println("Error data: " + reqErr.getData());
        } else {
          System.err.println("Unexpected error: " + err.getMessage());
        }
      });
  }

  public void sessionFromContext() {
    // Access the current session from Vert.x context
    Context ctx = Vertx.currentContext();
    ClientSession session = ClientSession.fromContext(ctx);

    if (session != null) {
      System.out.println("Current session: " + session.id());
      System.out.println("Session active: " + session.isActive());
      System.out.println("Streaming enabled: " + session.isStreaming());
    }
  }

  public void closeSession(ClientSession session) {
    // Close the session when done
    session.close(Promise.promise());
  }

  public void completeExample(Vertx vertx) {
    // 1. Configure client options
    ClientOptions options = new ClientOptions()
      .setClientName("example-client")
      .setClientVersion("1.0.0")
      .setStreamingEnabled(true);

    // 2. Create transport and client
    String serverUrl = "http://localhost:8080/mcp";
    ClientTransport transport = new StreamableHttpClientTransport(vertx, serverUrl,
      options);
    ModelContextProtocolClient client = ModelContextProtocolClient.create(vertx,
      transport, options);

    // 3. Add notification handlers
    client.addNotificationHandler("notifications/progress", notification -> {
      ProgressNotification progress = (ProgressNotification) notification;
      System.out.println("Progress: " + progress.getProgress());
    });

    // 4. Add client features
    RootsClientFeature rootsFeature = new RootsClientFeature()
      .addRoot(new Root()
        .setName("workspace")
        .setUri("file:///home/user/workspace"));
    client.addClientFeature(rootsFeature);

    // 5. Connect and use the client
    ClientCapabilities capabilities = new ClientCapabilities();

    client.subscribe(capabilities)
      .compose(session -> {
        System.out.println("Connected to server: " + session.id());

        // List tools
        return client.sendRequest(new ListToolsRequest())
          .compose(result -> {
            ListToolsResult tools = (ListToolsResult) result;
            System.out.println("Available tools: " + tools.getTools().size());

            // Call a tool
            CallToolRequest callRequest = new CallToolRequest()
              .setName("echo")
              .setArguments(new JsonObject().put("message", "Hello MCP!"));

            return client.sendRequest(callRequest);
          })
          .compose(result -> {
            CallToolResult callResult = (CallToolResult) result;
            System.out.println("Tool result: " + callResult.getContent());

            // Read a resource
            ReadResourceRequest readRequest = new ReadResourceRequest()
              .setUri("info://server");

            return client.sendRequest(readRequest);
          })
          .compose(result -> {
            ReadResourceResult readResult = (ReadResourceResult) result;
            System.out.println("Resource: " + readResult.getContents());

            // Close session when done
            Promise<Void> promise = Promise.promise();
            session.close(promise);
            return promise.future();
          });
      })
      .onSuccess(v -> {
        System.out.println("Demo completed successfully");
        vertx.close();
      })
      .onFailure(err -> {
        System.err.println("Error: " + err.getMessage());
        vertx.close();
      });
  }
}
