package examples;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.LoggingLevel;
import io.vertx.mcp.common.Meta;
import io.vertx.mcp.common.completion.Completion;
import io.vertx.mcp.common.content.Content;
import io.vertx.mcp.common.content.TextContent;
import io.vertx.mcp.common.notification.LoggingMessageNotification;
import io.vertx.mcp.common.prompt.PromptMessage;
import io.vertx.mcp.common.resources.TextResourceContent;
import io.vertx.mcp.server.ModelContextProtocolServer;
import io.vertx.mcp.server.ServerOptions;
import io.vertx.mcp.server.ServerSession;
import io.vertx.mcp.server.feature.LoggingServerFeature;
import io.vertx.mcp.server.feature.PromptServerFeature;
import io.vertx.mcp.server.feature.ResourceServerFeature;
import io.vertx.mcp.server.feature.ToolServerFeature;
import io.vertx.mcp.server.transport.http.StreamableHttpServerTransport;

import java.util.ArrayList;
import java.util.List;

import static io.vertx.json.schema.common.dsl.Schemas.*;

public class McpServerExamples {

  public void createServer(Vertx vertx) {
    // Create the MCP server
    ModelContextProtocolServer mcpServer = ModelContextProtocolServer.create(vertx);

    // Create HTTP transport
    StreamableHttpServerTransport transport = new StreamableHttpServerTransport(vertx, mcpServer);

    // Create and start HTTP server
    vertx.createHttpServer()
      .requestHandler(transport)
      .listen(8080)
      .onSuccess(server -> System.out.println(
        "MCP server started on port " + server.actualPort()));
  }

  public void createServerWithNameVersion(Vertx vertx) {
    ModelContextProtocolServer mcpServer = ModelContextProtocolServer.create(vertx,
      "my-server", "1.0.0");
  }

  public void createServerWithOptions(Vertx vertx) {
    ServerOptions options = new ServerOptions()
      .setServerName("my-mcp-server")
      .setServerVersion("2.0.0")
      .setSessionsEnabled(true)
      .setStreamingEnabled(true)
      .setSessionTimeoutMs(60 * 60 * 1000L); // 1 hour

    ModelContextProtocolServer mcpServer = ModelContextProtocolServer.create(vertx,
      options);
  }

  public void addUnstructuredTool(ModelContextProtocolServer mcpServer) {
    ToolServerFeature toolFeature = new ToolServerFeature();

    // Add a simple tool
    toolFeature.addUnstructuredTool(
      "greet",                              // name
      "Greet Tool",                               // title
      "Greets the user by name",                  // description
      objectSchema()                              // input schema
        .requiredProperty("name", stringSchema()),
      args -> {
        String name = args.getString("name");
        Content[] result = new Content[] {
          new TextContent("Hello, " + name + "!")
        };
        return Future.succeededFuture(result);
      }
    );

    mcpServer.addServerFeature(toolFeature);
  }

  public void addStructuredTool(ToolServerFeature toolFeature) {
    toolFeature.addStructuredTool(
      "calculate",                                    // name
      "Calculator",                                         // title
      "Performs basic arithmetic",                          // description
      objectSchema()                                        // input schema
        .requiredProperty("operation", stringSchema())
        .requiredProperty("a", numberSchema())
        .requiredProperty("b", numberSchema()),
      objectSchema()                                        // output schema
        .requiredProperty("result", numberSchema()),
      args -> {
        String op = args.getString("operation");
        double a = args.getDouble("a");
        double b = args.getDouble("b");
        double result;

        switch (op) {
          case "add":
            result = a + b;
            break;
          case "subtract":
            result = a - b;
            break;
          case "multiply":
            result = a * b;
            break;
          case "divide":
            result = a / b;
            break;
          default:
            return Future.failedFuture("Unknown operation: " + op);
        }

        return Future.succeededFuture(new JsonObject().put("result", result));
      }
    );
  }

  public void addStaticResource(ModelContextProtocolServer mcpServer) {
    ResourceServerFeature resourceFeature = new ResourceServerFeature();

    // Add a static resource
    resourceFeature.addStaticResource(
      "config://app/settings",                // URI
      "Application Settings",                     // name
      "Current application configuration",        // title
      "Returns the current app settings",         // description
      () -> {
        JsonObject config = new JsonObject().put("theme", "dark")
          .put("language", "en");
        return Future.succeededFuture(
          new TextResourceContent()
            .setUri("config://app/settings")
            .setName("app-settings")
            .setMimeType("application/json")
            .setText(config.encodePrettily())
        );
      }
    );

    mcpServer.addServerFeature(resourceFeature);
  }

  public void addDynamicResource(ResourceServerFeature resourceFeature) {
    // Add a dynamic resource with URI template
    resourceFeature.addDynamicResource(
      "user://profile/{userId}",              // URI template
      "User Profile",                             // name
      "User profile data",                        // title
      "Returns profile for the specified user",   // description
      params -> {
        String userId = params.get("userId");
        JsonObject profile = new JsonObject()
          .put("id", userId)
          .put("name", "User " + userId);
        return Future.succeededFuture(
          new TextResourceContent()
            .setUri("user://profile/" + userId)
            .setName("user-" + userId)
            .setText(profile.encode())
            .setMimeType("application/json")
        );
      }
    );
  }

  public void notifyResourceUpdated(Vertx vertx,
    ResourceServerFeature resourceFeature) {
    // When a resource changes, notify subscribed clients
    resourceFeature.notifyResourceUpdated(vertx, "config://app/settings");
  }

  public void addPrompt(ModelContextProtocolServer mcpServer) {
    PromptServerFeature promptFeature = new PromptServerFeature();

    // Add a prompt with arguments
    promptFeature.addPrompt(
      "code-review",                        // name
      "Code Review",                              // title
      "Reviews code for best practices",          // description
      arraySchema()                               // arguments schema
        .items(objectSchema()
          .requiredProperty("name", stringSchema())
          .requiredProperty("description", stringSchema())
          .requiredProperty("required", booleanSchema())),
      args -> {
        String code = args.getString("code");
        String language = args.getString("language", "java");

        List<PromptMessage> messages = new ArrayList<>();
        messages.add(new PromptMessage()
          .setRole("user")
          .setContent(new TextContent(
            "Please review this " + language + " code:\n\n```" + language + "\n" + code + "\n```"
          ).toJson())
        );

        return Future.succeededFuture(messages);
      }
    );

    mcpServer.addServerFeature(promptFeature);
  }

  public void configureServerOptions() {
    ServerOptions options = new ServerOptions()
      .setServerName("my-server")
      .setServerVersion("1.0.0")
      .setSessionsEnabled(true)
      .setStreamingEnabled(true)
      .setSessionTimeoutMs(30 * 60 * 1000L)
      .setMaxSessions(500);
  }

  public void createTransport(Vertx vertx, ModelContextProtocolServer mcpServer) {
    StreamableHttpServerTransport transport = new StreamableHttpServerTransport(vertx, mcpServer);

    // Use with a Vert.x HTTP server
    vertx.createHttpServer()
      .requestHandler(transport)
      .listen(8080);
  }

  public void createTransportWithCors(Vertx vertx, ModelContextProtocolServer mcpServer) {
    StreamableHttpServerTransport transport = new StreamableHttpServerTransport(vertx, mcpServer);

    vertx.createHttpServer()
      .requestHandler(req -> {
        // Add CORS headers to all responses
        req.response()
          .putHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600")
          .putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
          .putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, DELETE, OPTIONS")
          .putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
            String.join(",", StreamableHttpServerTransport.ACCEPTED_HEADERS))
          .putHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
            String.join(",", StreamableHttpServerTransport.ACCEPTED_HEADERS));

        // Handle CORS preflight requests
        if (req.method() == HttpMethod.OPTIONS) {
          req.response().setStatusCode(200).end();
          return;
        }
        // Pass request to transport
        transport.handle(req);
      })
      .listen(8080);
  }

  public void addDynamicResourceWithCompletion(
    ResourceServerFeature resourceFeature) {
    // Add a dynamic resource with completion support
    resourceFeature.addDynamicResource(
      "file://project/{path}",
      "Project File",
      "File from project",
      "Returns a file from the project directory",
      params -> {
        String path = params.get("path");
        return Future.succeededFuture(
          new TextResourceContent()
            .setUri("file://project/" + path)
            .setName("file-" + path)
            .setText("File content here")
        );
      },
      (argument, context) -> {
        // Provide path completions
        String partial = argument.getValue() != null ? argument.getValue() : "";
        List<String> allPaths = new ArrayList<>();
        allPaths.add("src/main/java");
        allPaths.add("src/test/java");
        allPaths.add("pom.xml");

        List<String> matches = new ArrayList<>();
        for (String path : allPaths) {
          if (path.startsWith(partial)) {
            matches.add(path);
          }
        }

        return Future.succeededFuture(new Completion()
          .setValues(matches)
          .setTotal(matches.size())
          .setHasMore(false));
      }
    );
  }

  public void completeExample(Vertx vertx) {
    // Create server with options
    ServerOptions options = new ServerOptions()
      .setServerName("example-server")
      .setServerVersion("1.0.0");

    ModelContextProtocolServer mcpServer = ModelContextProtocolServer.create(vertx,
      options);

    // Add tools
    ToolServerFeature tools = new ToolServerFeature();
    tools.addUnstructuredTool(
      "echo",
      "Echo Tool",
      "Echoes back the input",
      objectSchema().requiredProperty("message", stringSchema()),
      args -> Future.succeededFuture(new Content[] {
        new TextContent(args.getString("message"))
      })
    );
    mcpServer.addServerFeature(tools);

    // Add resources
    ResourceServerFeature resources = new ResourceServerFeature();
    resources.addStaticResource(
      "info://server",
      "Server Info",
      () -> Future.succeededFuture(
        new TextResourceContent()
          .setUri("info://server")
          .setName("server-info")
          .setText("Example MCP Server v1.0.0")
      )
    );
    mcpServer.addServerFeature(resources);

    // Create transport and start server
    StreamableHttpServerTransport transport = new StreamableHttpServerTransport(vertx, mcpServer);

    vertx.createHttpServer()
      .requestHandler(transport)
      .listen(8080)
      .onSuccess(server ->
        System.out.println(
          "MCP server running on http://localhost:" + server.actualPort() + "/mcp")
      )
      .onFailure(err ->
        System.err.println("Failed to start server: " + err.getMessage())
      );
  }

  public void accessSession() {
    Context ctx = Vertx.currentContext();
    ServerSession session = ServerSession.fromContext(ctx);
  }

  public void useSessionInTool(ToolServerFeature toolFeature) {
    toolFeature.addStructuredTool(
      "session-aware-tool",
      "Session Aware Tool",
      "A tool that uses the session",
      objectSchema(),
      objectSchema().requiredProperty("sessionId", stringSchema()),
      args -> {
        Context ctx = Vertx.currentContext();
        ServerSession session = ServerSession.fromContext(ctx);

        if (session != null) {
          // Send a notification to the client
          LoggingMessageNotification notification = new LoggingMessageNotification()
            .setLevel(LoggingLevel.INFO)
            .setLogger("session-aware-tool")
            .setData("Processing your request");

          session.sendNotification(notification);

          return Future.succeededFuture(
            new JsonObject().put("sessionId", session.id())
          );
        }

        return Future.succeededFuture(
          new JsonObject().put("sessionId", "no-session")
        );
      }
    );
  }

  public void accessMeta() {
    Context ctx = Vertx.currentContext();
    JsonObject meta = Meta.fromContext(ctx);
  }

  public void useMetaInTool(ToolServerFeature toolFeature) {
    toolFeature.addStructuredTool(
      "metadata-tool",
      "Metadata Tool",
      "A tool that uses request metadata",
      objectSchema(),
      objectSchema().requiredProperty("received", booleanSchema()),
      args -> {
        Context ctx = Vertx.currentContext();
        JsonObject requestMeta = Meta.fromContext(ctx);

        boolean hasMetadata = requestMeta != null;
        if (hasMetadata) {
          String requestId = requestMeta.getString("requestId");
          // Use metadata for logging or correlation
        }

        return Future.succeededFuture(
          new JsonObject().put("received", hasMetadata)
        );
      }
    );
  }
}
