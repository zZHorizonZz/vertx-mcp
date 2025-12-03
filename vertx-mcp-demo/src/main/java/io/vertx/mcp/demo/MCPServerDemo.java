package io.vertx.mcp.demo;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.common.dsl.Schemas;
import io.vertx.mcp.common.LoggingLevel;
import io.vertx.mcp.common.Meta;
import io.vertx.mcp.common.completion.Completion;
import io.vertx.mcp.common.content.Content;
import io.vertx.mcp.common.content.TextContent;
import io.vertx.mcp.common.notification.LoggingMessageNotification;
import io.vertx.mcp.common.notification.ProgressNotification;
import io.vertx.mcp.common.prompt.PromptMessage;
import io.vertx.mcp.common.request.CreateMessageRequest;
import io.vertx.mcp.common.request.ElicitRequest;
import io.vertx.mcp.common.resources.TextResourceContent;
import io.vertx.mcp.common.sampling.SamplingMessage;
import io.vertx.mcp.server.ModelContextProtocolServer;
import io.vertx.mcp.server.PromptHandler;
import io.vertx.mcp.server.ServerOptions;
import io.vertx.mcp.server.ServerSession;
import io.vertx.mcp.server.feature.CompletionServerFeature;
import io.vertx.mcp.server.feature.PromptServerFeature;
import io.vertx.mcp.server.feature.ResourceServerFeature;
import io.vertx.mcp.server.feature.ToolServerFeature;
import io.vertx.mcp.server.transport.http.StreamableHttpServerTransport;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * A comprehensive MCP server demo showcasing all major features of the Model Context Protocol.
 *
 * <p>This demo simulates a task management system that demonstrates:
 * <ul>
 *   <li><b>Tools</b> - Task CRUD operations with structured/unstructured outputs</li>
 *   <li><b>Resources</b> - Static and dynamic resource access with completions</li>
 *   <li><b>Prompts</b> - Pre-built prompt templates with argument completions</li>
 *   <li><b>Sampling</b> - Request LLM responses from the client</li>
 *   <li><b>Elicitation</b> - Request user input with schema validation</li>
 *   <li><b>Progress</b> - Report progress during long-running operations</li>
 *   <li><b>Logging</b> - Send log notifications to the client</li>
 * </ul>
 */
public class MCPServerDemo {

  private static final Map<String, JsonObject> tasks = new ConcurrentHashMap<>();
  private static final List<String> priorities = Arrays.asList("low", "medium", "high", "critical");
  private static final List<String> statuses = Arrays.asList("todo", "in_progress", "review", "done");

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();

    ServerOptions serverOptions = new ServerOptions()
      .setServerName("task-manager-mcp")
      .setServerVersion("1.0.0")
      .setSessionsEnabled(true)
      .setStreamingEnabled(true);

    ModelContextProtocolServer mcpServer = ModelContextProtocolServer.create(vertx, serverOptions);

    // Initialize with sample data
    initializeSampleData();

    // Setup all features
    mcpServer.addServerFeature(setupTools());
    mcpServer.addServerFeature(setupResources());
    mcpServer.addServerFeature(setupPrompts());
    mcpServer.addServerFeature(new CompletionServerFeature(mcpServer));

    // Create and start HTTP server
    StreamableHttpServerTransport transport = new StreamableHttpServerTransport(vertx, mcpServer);
    int port = Integer.parseInt(System.getProperty("port", "8080"));

    HttpServer server = vertx.createHttpServer(new HttpServerOptions()
      .setPort(port)
      .setHost("0.0.0.0"));

    server.requestHandler(req -> {
      HttpServerResponse response = req.response();
      response.putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
      response.putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, DELETE, OPTIONS");
      response.putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, String.join(",", StreamableHttpServerTransport.ACCEPTED_HEADERS));
      response.putHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, String.join(",", StreamableHttpServerTransport.ACCEPTED_HEADERS));

      if (req.method() == HttpMethod.OPTIONS) {
        req.response().setStatusCode(200).end();
        return;
      }
      transport.handle(req);
    });

    server.listen().onSuccess(s -> {
      System.out.println("Task Manager MCP Server started on http://localhost:" + s.actualPort() + "/mcp");
      System.out.println("\nFeatures: Tools, Resources, Prompts, Sampling, Elicitation, Progress, Logging");
    }).onFailure(err -> {
      System.err.println("Failed to start server: " + err.getMessage());
      vertx.close();
    });
  }

  private static void initializeSampleData() {
    tasks.put("task-1", new JsonObject()
      .put("id", "task-1")
      .put("title", "Review mass of pull requests")
      .put("description", "They keep coming, send help")
      .put("priority", "critical")
      .put("status", "in_progress")
      .put("assignee", "julien@vertx.io")
      .put("created", "2038-01-19T03:14:07")); // Y2K38 bug date

    tasks.put("task-2", new JsonObject()
      .put("id", "task-2")
      .put("title", "Write documentation nobody will read")
      .put("description", "But at least we can say we have docs")
      .put("priority", "high")
      .put("status", "todo")
      .put("assignee", "daniel@vertx.io")
      .put("created", "1999-12-31T23:59:59")); // Y2K eve

    tasks.put("task-3", new JsonObject()
      .put("id", "task-3")
      .put("title", "Add more event loops")
      .put("description", "Because 2x CPU cores is never enough")
      .put("priority", "medium")
      .put("status", "review")
      .put("assignee", "thomas@vertx.io")
      .put("created", "2015-10-21T16:29:00")); // Back to the Future Day
  }

  /**
   * Configures tools for task management operations.
   */
  private static ToolServerFeature setupTools() {
    ToolServerFeature tools = new ToolServerFeature();

    // Create task with elicitation for missing fields
    tools.addUnstructuredTool(
      "create_task",
      "Create Task",
      "Creates a new task, prompting for required information if not provided",
      Schemas.objectSchema()
        .property("title", Schemas.stringSchema())
        .property("description", Schemas.stringSchema())
        .property("priority", Schemas.stringSchema().withKeyword("enum", new JsonArray(priorities)))
        .property("assignee", Schemas.stringSchema()),
      args -> {
        Context context = Vertx.currentContext();
        ServerSession session = ServerSession.fromContext(context);

        String title = args.getString("title");
        String description = args.getString("description");

        // Use elicitation if required fields are missing
        if ((title == null || description == null) && session != null) {
          JsonObject schema = new JsonObject()
            .put("type", "object")
            .put("properties", new JsonObject()
              .put("title", new JsonObject()
                .put("type", "string")
                .put("description", "Task title")
                .put("default", title != null ? title : ""))
              .put("description", new JsonObject()
                .put("type", "string")
                .put("description", "Task description")
                .put("default", description != null ? description : ""))
              .put("priority", new JsonObject()
                .put("type", "string")
                .put("enum", new JsonArray(priorities))
                .put("default", "medium"))
              .put("assignee", new JsonObject()
                .put("type", "string")
                .put("description", "Email of assignee")))
            .put("required", new JsonArray().add("title").add("description"));

          return session.sendRequest(new ElicitRequest()
              .setMessage("Please provide task details:")
              .setRequestedSchema(schema))
            .compose(response -> {
              if (!"confirm".equals(response.getString("action"))) {
                return Future.succeededFuture(new Content[] {
                  new TextContent("Task creation cancelled.")
                });
              }
              JsonObject content = response.getJsonObject("content", new JsonObject());
              return createTaskInternal(content);
            });
        }

        return createTaskInternal(args);
      }
    );

    // List tasks with filtering
    tools.addStructuredTool(
      "list_tasks",
      "List Tasks",
      "Lists all tasks with optional filtering by status or priority",
      Schemas.objectSchema()
        .property("status", Schemas.stringSchema().withKeyword("enum", new JsonArray(statuses)))
        .property("priority", Schemas.stringSchema().withKeyword("enum", new JsonArray(priorities))),
      Schemas.objectSchema()
        .property("tasks", Schemas.arraySchema())
        .property("count", Schemas.intSchema()),
      args -> {
        String statusFilter = args.getString("status");
        String priorityFilter = args.getString("priority");

        List<JsonObject> filtered = tasks.values().stream()
          .filter(t -> statusFilter == null || statusFilter.equals(t.getString("status")))
          .filter(t -> priorityFilter == null || priorityFilter.equals(t.getString("priority")))
          .collect(Collectors.toList());

        return Future.succeededFuture(new JsonObject()
          .put("tasks", new JsonArray(filtered))
          .put("count", filtered.size()));
      }
    );

    // Update task status
    tools.addStructuredTool(
      "update_task",
      "Update Task",
      "Updates a task's status or priority",
      Schemas.objectSchema()
        .requiredProperty("id", Schemas.stringSchema())
        .property("status", Schemas.stringSchema().withKeyword("enum", new JsonArray(statuses)))
        .property("priority", Schemas.stringSchema().withKeyword("enum", new JsonArray(priorities))),
      Schemas.objectSchema()
        .property("success", Schemas.booleanSchema())
        .property("task", Schemas.objectSchema()),
      args -> {
        String id = args.getString("id");
        JsonObject task = tasks.get(id);

        if (task == null) {
          return Future.succeededFuture(new JsonObject()
            .put("success", false)
            .put("error", "Task not found: " + id));
        }

        if (args.containsKey("status")) {
          task.put("status", args.getString("status"));
        }
        if (args.containsKey("priority")) {
          task.put("priority", args.getString("priority"));
        }

        return Future.succeededFuture(new JsonObject()
          .put("success", true)
          .put("task", task));
      }
    );

    // Bulk import with progress reporting
    tools.addUnstructuredTool(
      "bulk_import",
      "Bulk Import Tasks",
      "Imports multiple tasks with progress reporting",
      Schemas.objectSchema()
        .requiredProperty("tasks", Schemas.arraySchema()),
      args -> {
        Context context = Vertx.currentContext();
        ServerSession session = ServerSession.fromContext(context);
        JsonObject meta = Meta.fromContext(context);
        JsonArray taskList = args.getJsonArray("tasks", new JsonArray());

        if (session == null || meta == null || meta.getString("progressToken") == null) {
          // No progress support, import directly
          int count = 0;
          for (int i = 0; i < taskList.size(); i++) {
            JsonObject t = taskList.getJsonObject(i);
            String id = "task-" + System.currentTimeMillis() + "-" + i;
            t.put("id", id);
            t.put("created", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            tasks.put(id, t);
            count++;
          }
          return Future.succeededFuture(new Content[] {
            new TextContent("Imported " + count + " tasks.")
          });
        }

        String token = meta.getString("progressToken");
        int total = taskList.size();

        return importWithProgress(session, token, taskList, 0, total, new ArrayList<>());
      }
    );

    // Summarize tasks using sampling
    tools.addUnstructuredTool(
      "summarize_tasks",
      "Summarize Tasks",
      "Uses LLM to generate a summary of current tasks",
      Schemas.objectSchema(),
      args -> {
        Context context = Vertx.currentContext();
        ServerSession session = ServerSession.fromContext(context);

        if (session == null) {
          return Future.succeededFuture(new Content[] {
            new TextContent("Sampling not available - no session context.")
          });
        }

        // Log the action
        return session.sendNotification(new LoggingMessageNotification()
            .setLevel(LoggingLevel.INFO)
            .setLogger("task-manager")
            .setData("Requesting task summary via sampling"))
          .compose(v -> {
            String taskData = tasks.values().stream()
              .map(t -> String.format("- [%s] %s (%s, %s)",
                t.getString("id"),
                t.getString("title"),
                t.getString("priority"),
                t.getString("status")))
              .collect(Collectors.joining("\n"));

            SamplingMessage message = new SamplingMessage()
              .setRole("user")
              .setContent(new JsonObject()
                .put("type", "text")
                .put("text", "Summarize these tasks in 2-3 sentences:\n\n" + taskData));

            return session.sendRequest(new CreateMessageRequest()
              .setMessages(List.of(message))
              .setMaxTokens(200));
          })
          .map(response -> {
            JsonObject content = response.getJsonObject("content");
            String text = content != null ? content.getString("text", "No summary generated") : "No summary generated";
            return new Content[] { new TextContent(text) };
          });
      }
    );

    return tools;
  }

  private static Future<Content[]> createTaskInternal(JsonObject args) {
    String id = "task-" + System.currentTimeMillis();
    JsonObject task = new JsonObject()
      .put("id", id)
      .put("title", args.getString("title"))
      .put("description", args.getString("description"))
      .put("priority", args.getString("priority", "medium"))
      .put("status", "todo")
      .put("assignee", args.getString("assignee", ""))
      .put("created", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

    tasks.put(id, task);

    return Future.succeededFuture(new Content[] {
      new TextContent("Created task: " + id + " - " + task.getString("title"))
    });
  }

  private static Future<Content[]> importWithProgress(ServerSession session, String token, JsonArray taskList, int index, int total, List<String> imported) {
    if (index >= total) {
      return Future.succeededFuture(new Content[] {
        new TextContent("Imported " + imported.size() + " tasks: " + String.join(", ", imported))
      });
    }

    return session.sendNotification(new ProgressNotification()
        .setProgressToken(token)
        .setProgress((double) index)
        .setTotal((double) total))
      .compose(v -> {
        JsonObject t = taskList.getJsonObject(index);
        String id = "task-" + System.currentTimeMillis() + "-" + index;
        t.put("id", id);
        t.put("created", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        tasks.put(id, t);
        imported.add(id);

        // Small delay to simulate work
        return Future.future(promise ->
          Vertx.currentContext().owner().setTimer(100, x -> promise.complete()));
      })
      .compose(v -> importWithProgress(session, token, taskList, index + 1, total, imported));
  }

  /**
   * Configures resources for accessing task data.
   */
  private static ResourceServerFeature setupResources() {
    ResourceServerFeature resources = new ResourceServerFeature();

    // All tasks as JSON
    resources.addStaticResource("tasks://all", () ->
      Future.succeededFuture(new TextResourceContent()
        .setUri("tasks://all")
        .setName("All Tasks")
        .setMimeType("application/json")
        .setText(new JsonArray(new ArrayList<>(tasks.values())).encodePrettily()))
    );

    // Individual task by ID with completion
    resources.addDynamicResource(
      "tasks://task/{id}",
      "task",
      "Task Details",
      "Get details of a specific task by ID",
      params -> {
        String id = params.get("id");
        JsonObject task = tasks.get(id);

        if (task == null) {
          return Future.failedFuture("Task not found: " + id);
        }

        return Future.succeededFuture(new TextResourceContent()
          .setUri("tasks://task/" + id)
          .setName(task.getString("title"))
          .setMimeType("application/json")
          .setText(task.encodePrettily()));
      },
      (argument, context) -> {
        if ("id".equals(argument.getName())) {
          String prefix = argument.getValue() != null ? argument.getValue() : "";
          List<String> ids = tasks.keySet().stream()
            .filter(id -> id.contains(prefix))
            .collect(Collectors.toList());
          return Future.succeededFuture(new Completion()
            .setValues(ids)
            .setTotal(ids.size())
            .setHasMore(false));
        }
        return Future.succeededFuture(new Completion().setValues(new ArrayList<>()).setTotal(0).setHasMore(false));
      }
    );

    // Tasks by status
    resources.addDynamicResource(
      "tasks://status/{status}",
      "tasks-by-status",
      "Tasks by Status",
      "Get all tasks with a specific status",
      params -> {
        String status = params.get("status");
        List<JsonObject> filtered = tasks.values().stream()
          .filter(t -> status.equals(t.getString("status")))
          .collect(Collectors.toList());

        return Future.succeededFuture(new TextResourceContent()
          .setUri("tasks://status/" + status)
          .setName("Tasks - " + status)
          .setMimeType("application/json")
          .setText(new JsonArray(filtered).encodePrettily()));
      },
      (argument, context) -> {
        if ("status".equals(argument.getName())) {
          String prefix = argument.getValue() != null ? argument.getValue().toLowerCase() : "";
          List<String> matches = statuses.stream()
            .filter(s -> s.startsWith(prefix))
            .collect(Collectors.toList());
          return Future.succeededFuture(new Completion()
            .setValues(matches)
            .setTotal(matches.size())
            .setHasMore(false));
        }
        return Future.succeededFuture(new Completion().setValues(new ArrayList<>()).setTotal(0).setHasMore(false));
      }
    );

    return resources;
  }

  /**
   * Configures prompts for common task operations.
   */
  private static PromptServerFeature setupPrompts() {
    PromptServerFeature prompts = new PromptServerFeature();

    // Daily standup report
    prompts.addPrompt(
      "daily_standup",
      "Daily Standup",
      "Generates a standup report from current tasks",
      Schemas.arraySchema().items(
        Schemas.objectSchema().property("assignee", Schemas.stringSchema())
      ),
      args -> {
        String assignee = args.getString("assignee");

        List<JsonObject> userTasks = tasks.values().stream()
          .filter(t -> assignee == null || assignee.equals(t.getString("assignee")))
          .collect(Collectors.toList());

        long inProgress = userTasks.stream().filter(t -> "in_progress".equals(t.getString("status"))).count();
        long done = userTasks.stream().filter(t -> "done".equals(t.getString("status"))).count();
        long blocked = userTasks.stream().filter(t -> "review".equals(t.getString("status"))).count();

        String taskSummary = userTasks.stream()
          .map(t -> String.format("- %s: %s [%s]", t.getString("id"), t.getString("title"), t.getString("status")))
          .collect(Collectors.joining("\n"));

        List<PromptMessage> messages = new ArrayList<>();
        messages.add(new PromptMessage()
          .setRole("user")
          .setContent(new TextContent(String.format(
            "Generate a standup report for %s:\n\nStats: %d in progress, %d completed, %d in review\n\nTasks:\n%s",
            assignee != null ? assignee : "the team",
            inProgress, done, blocked, taskSummary
          )).toJson()));

        return Future.succeededFuture(messages);
      }
    );

    // Sprint planning prompt with completions
    prompts.addPrompt(PromptHandler.create(
      "sprint_planning",
      "Sprint Planning",
      "Helps plan the next sprint based on current backlog",
      Schemas.arraySchema().items(
        Schemas.objectSchema()
          .requiredProperty("priority", Schemas.stringSchema().withKeyword("enum", new JsonArray(priorities)))
          .property("capacity", Schemas.intSchema())
      ),
      args -> {
        String priority = args.getString("priority", "high");
        int capacity = args.getInteger("capacity", 5);

        List<JsonObject> candidates = tasks.values().stream()
          .filter(t -> "todo".equals(t.getString("status")))
          .filter(t -> priorities.indexOf(t.getString("priority")) >= priorities.indexOf(priority))
          .limit(capacity)
          .collect(Collectors.toList());

        String taskList = candidates.stream()
          .map(t -> String.format("- %s: %s (Priority: %s)", t.getString("id"), t.getString("title"), t.getString("priority")))
          .collect(Collectors.joining("\n"));

        List<PromptMessage> messages = new ArrayList<>();
        messages.add(new PromptMessage()
          .setRole("user")
          .setContent(new TextContent(String.format(
            "Plan the next sprint with capacity for %d tasks. Focus on %s priority and above.\n\nCandidate tasks:\n%s\n\nProvide recommendations on which tasks to include and why.",
            capacity, priority, taskList.isEmpty() ? "No matching tasks found" : taskList
          )).toJson()));

        return Future.succeededFuture(messages);
      },
      (argument, context) -> {
        if ("priority".equals(argument.getName())) {
          String prefix = argument.getValue() != null ? argument.getValue().toLowerCase() : "";
          List<String> matches = priorities.stream()
            .filter(p -> p.startsWith(prefix))
            .collect(Collectors.toList());
          return Future.succeededFuture(new Completion()
            .setValues(matches)
            .setTotal(matches.size())
            .setHasMore(false));
        }
        return Future.succeededFuture(new Completion().setValues(new ArrayList<>()).setTotal(0).setHasMore(false));
      }
    ));

    return prompts;
  }
}
